package com.olegbelyanin.expensetracker.database

import androidx.room3.withWriteTransaction
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.entities.CategoryEntity
import com.olegbelyanin.expensetracker.database.learning.CategoryNameExperienceWriter
import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.category.BuiltinCategoryLockedException
import com.olegbelyanin.expensetracker.domain.category.CategoryNotFoundException
import com.olegbelyanin.expensetracker.domain.category.DuplicateCategoryNameException
import com.olegbelyanin.expensetracker.domain.category.EmptyCategoryNameException
import com.olegbelyanin.expensetracker.domain.category.FallbackCategoryProtectedException
import com.olegbelyanin.expensetracker.model.BuiltinCategories
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.CategoryIcons
import com.olegbelyanin.expensetracker.model.CategoryPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

class RoomCategoryRepository(
    private val database: AppDatabase,
    private val normalizer: TextNormalizer,
    private val clock: Clock,
) : CategoryRepository {
    private val nameExperience = CategoryNameExperienceWriter(database, normalizer)

    override suspend fun getActiveCategories(): List<Category> =
        database.categoryDao().getActive().map { it.toDomain() }

    override fun observeActiveCategories(): Flow<List<Category>> =
        database.categoryDao().observeActive().map { rows -> rows.map { it.toDomain() } }

    override fun observeArchivedCategories(): Flow<List<Category>> =
        database.categoryDao().observeArchived().map { rows -> rows.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Category>> =
        database.categoryDao().observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: Long): Category? = database.categoryDao().findById(id)?.toDomain()

    override suspend fun requireFallback(): Category {
        val entity = database.categoryDao().findBuiltinByCode(BuiltinCategories.FALLBACK_CODE)
            ?: error("Fallback category ${BuiltinCategories.FALLBACK_CODE} is missing")
        return entity.toDomain()
    }

    override suspend fun createUserCategory(name: String, color: String, icon: String): Category =
        database.withWriteTransaction { persistUser(id = null, name = name, color = color, icon = icon) }

    override suspend fun updateUserCategory(id: Long, name: String, color: String, icon: String): Category =
        database.withWriteTransaction { persistUser(id = id, name = name, color = color, icon = icon) }

    override suspend fun archive(id: Long): Category = database.withWriteTransaction {
        val current = database.categoryDao().findById(id) ?: throw CategoryNotFoundException(id)
        val domain = current.toDomain()
        if (domain.isFallback) throw FallbackCategoryProtectedException()
        if (current.archivedAt != null) return@withWriteTransaction domain
        val now = Instant.now(clock).toEpochMilli()
        val archived = current.copy(archivedAt = now, updatedAt = now)
        database.categoryDao().update(archived)
        archived.toDomain()
    }

    override suspend fun restore(id: Long): Category = database.withWriteTransaction {
        val current = database.categoryDao().findById(id) ?: throw CategoryNotFoundException(id)
        if (current.archivedAt == null) return@withWriteTransaction current.toDomain()
        val now = Instant.now(clock).toEpochMilli()
        val restored = current.copy(archivedAt = null, updatedAt = now)
        database.categoryDao().update(restored)
        nameExperience.writeIfMissing(restored.id, restored.name)
        restored.toDomain()
    }

    private suspend fun persistUser(id: Long?, name: String, color: String, icon: String): Category {
        val analysis = normalizer.analyze(name)
        if (analysis.normalizedName.isEmpty()) throw EmptyCategoryNameException()
        val now = Instant.now(clock).toEpochMilli()
        val storedColor = color.ifBlank { CategoryPalette.DEFAULT }
        val storedIcon = CategoryIcons.canonicalize(icon)
        if (id != null) {
            return updateExisting(id, name.trim(), analysis.normalizedName, storedColor, storedIcon, now)
        }
        return insertOrReactivate(name.trim(), analysis.normalizedName, storedColor, storedIcon, now)
    }

    private suspend fun updateExisting(
        id: Long,
        name: String,
        normalizedName: String,
        color: String,
        icon: String,
        now: Long,
    ): Category {
        val current = database.categoryDao().findById(id) ?: throw CategoryNotFoundException(id)
        if (current.isBuiltin) throw BuiltinCategoryLockedException(id)
        val clash = database.categoryDao().findByNormalizedName(normalizedName)
        if (clash != null && clash.id != id) throw DuplicateCategoryNameException(name)
        val renamed = current.normalizedName != normalizedName
        val updated = current.copy(
            name = name,
            normalizedName = normalizedName,
            color = color,
            icon = icon,
            updatedAt = now,
        )
        database.categoryDao().update(updated)
        if (renamed) {
            nameExperience.replace(id, name)
        } else {
            nameExperience.writeIfMissing(id, name)
        }
        return updated.toDomain()
    }

    private suspend fun insertOrReactivate(
        name: String,
        normalizedName: String,
        color: String,
        icon: String,
        now: Long,
    ): Category {
        val existing = database.categoryDao().findByNormalizedName(normalizedName)
        if (existing != null) {
            if (existing.archivedAt == null) throw DuplicateCategoryNameException(name)
            val restored = if (existing.isBuiltin) {
                existing.copy(archivedAt = null, updatedAt = now)
            } else {
                existing.copy(
                    name = name,
                    color = color,
                    icon = icon,
                    archivedAt = null,
                    updatedAt = now,
                )
            }
            database.categoryDao().update(restored)
            nameExperience.writeIfMissing(restored.id, restored.name)
            return restored.toDomain()
        }
        val insertedId = database.categoryDao().insert(
            CategoryEntity(
                code = null,
                name = name,
                normalizedName = normalizedName,
                color = color,
                icon = icon,
                isBuiltin = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        nameExperience.writeIfMissing(insertedId, name)
        return database.categoryDao().findById(insertedId)?.toDomain()
            ?: error("Inserted category $insertedId was not found")
    }
}
