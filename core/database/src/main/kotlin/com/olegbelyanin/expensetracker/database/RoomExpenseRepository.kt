package com.olegbelyanin.expensetracker.database

import androidx.room3.withWriteTransaction
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.entities.ExpenseEntity
import com.olegbelyanin.expensetracker.database.entities.LocationEntity
import com.olegbelyanin.expensetracker.database.entities.LocationFtsEntity
import com.olegbelyanin.expensetracker.database.learning.LearningFingerprint
import com.olegbelyanin.expensetracker.database.learning.LearningPlanner
import com.olegbelyanin.expensetracker.database.learning.LearningWriter
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.PersistExpenseRequest
import com.olegbelyanin.expensetracker.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

class RoomExpenseRepository(
    private val database: AppDatabase,
    private val normalizer: TextNormalizer,
    private val clock: Clock,
) : ExpenseRepository {
    private val learningWriter = LearningWriter(database, normalizer)

    override suspend fun get(id: String): Expense? = database.expenseDao().findById(id)?.toDomain()

    override suspend fun getAll(): List<Expense> = database.expenseDao().getAll().map { it.toDomain() }

    override suspend fun findByDedupKey(dedupKey: String): Expense? =
        database.expenseDao().findByDedupKey(dedupKey)?.toDomain()

    override fun observeAll(): Flow<List<Expense>> =
        database.expenseDao().observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun persist(request: PersistExpenseRequest): Expense = database.withWriteTransaction {
        persistInTransaction(request)
    }

    override suspend fun delete(id: String) {
        database.withWriteTransaction {
            val existing = database.expenseDao().findById(id) ?: return@withWriteTransaction
            existing.locationId?.let { decrementLocationUsage(it, Instant.now(clock).toEpochMilli()) }
            database.expenseDao().deleteById(id)
        }
    }

    override suspend fun clearHistory(): Int = database.withWriteTransaction {
        val deleted = database.expenseDao().getAll().size
        database.learningDao().detachExamplesFromExpenses()
        database.expenseDao().deleteAll()
        deleted
    }

    private suspend fun persistInTransaction(request: PersistExpenseRequest): Expense {
        val now = Instant.now(clock).toEpochMilli()
        val existing = database.expenseDao().findById(request.id)
        val analysis = normalizer.analyze(request.name)
        val locationId = resolveLocationId(request.locationName, now)
        val nextFingerprint = LearningFingerprint(
            normalizedName = analysis.normalizedName,
            categoryId = request.categoryId,
            locationId = locationId,
        )
        val previousFingerprint = existing?.let {
            LearningFingerprint(
                normalizedName = it.normalizedName,
                categoryId = it.categoryId,
                locationId = it.locationId,
            )
        }
        val plan = LearningPlanner.plan(
            hadExpense = existing != null,
            previous = previousFingerprint,
            next = nextFingerprint,
            source = request.categoryAssignmentSource,
            interactive = request.interactive,
            proposedCategoryId = request.proposedCategoryId,
        )
        val unchangedExpense = existing != null &&
            existing.amountMinor == request.amount.minor &&
            existing.spentAt == request.spentAt.toEpochMilli() &&
            existing.name == request.name &&
            existing.categoryId == request.categoryId &&
            existing.locationId == locationId &&
            existing.comment == request.comment &&
            existing.categoryAssignmentSource == request.categoryAssignmentSource.storageValue()
        if (unchangedExpense && !plan.writeLearning && plan.bumpLocationId == null && !plan.writeExactRule) {
            return existing.toDomain()
        }
        plan.unbumpLocationId?.let { decrementLocationUsage(it, now) }
        plan.bumpLocationId?.let { incrementLocationUsage(it, now) }
        val entity = ExpenseEntity(
            id = request.id,
            amountMinor = request.amount.minor,
            spentAt = request.spentAt.toEpochMilli(),
            name = request.name,
            normalizedName = analysis.normalizedName,
            categoryId = request.categoryId,
            locationId = locationId,
            comment = request.comment,
            categoryAssignmentSource = request.categoryAssignmentSource.storageValue(),
            dedupKey = existing?.dedupKey ?: request.dedupKey ?: userDedupKey(request.id),
            createdAt = existing?.createdAt ?: now,
            updatedAt = existing?.updatedAt?.takeIf { unchangedExpense } ?: now,
        )
        if (existing == null) {
            database.expenseDao().insert(entity)
        } else if (!unchangedExpense) {
            database.expenseDao().update(entity)
        }
        learningWriter.apply(
            expenseId = request.id,
            normalizedName = analysis.normalizedName,
            rawName = request.name,
            categoryId = request.categoryId,
            locationId = locationId,
            proposedCategoryId = request.proposedCategoryId ?: existing?.categoryId,
            plan = plan,
            now = now,
            activeCategoryIds = database.categoryDao().getActive().map { it.id }.toSet(),
        )
        return entity.toDomain()
    }

    private suspend fun resolveLocationId(rawName: String?, now: Long): Long? {
        val name = rawName?.trim().orEmpty()
        if (name.isEmpty()) return null
        val normalized = normalizer.normalizePlain(name)
        if (normalized.isEmpty()) return null
        val existing = database.locationDao().findByNormalizedName(normalized)
        val id = if (existing == null) {
            database.locationDao().insert(
                LocationEntity(
                    name = name,
                    normalizedName = normalized,
                    usageCount = 0,
                    lastUsedAt = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val reactivated = existing.copy(
                name = name,
                archivedAt = null,
                updatedAt = now,
            )
            if (reactivated != existing) {
                database.locationDao().update(reactivated)
            }
            existing.id
        }
        syncLocationFts(id, name, normalized)
        return id
    }

    private suspend fun incrementLocationUsage(id: Long, now: Long) {
        val location = database.locationDao().findById(id) ?: return
        database.locationDao().update(
            location.copy(
                usageCount = location.usageCount + 1,
                lastUsedAt = now,
                archivedAt = null,
                updatedAt = now,
            ),
        )
    }

    private suspend fun decrementLocationUsage(id: Long, now: Long) {
        val location = database.locationDao().findById(id) ?: return
        database.locationDao().update(
            location.copy(
                usageCount = (location.usageCount - 1).coerceAtLeast(0),
                updatedAt = now,
            ),
        )
    }

    private suspend fun syncLocationFts(id: Long, name: String, normalizedName: String) {
        val rowId = id.toInt()
        database.locationFtsDao().deleteByRowId(rowId)
        database.locationFtsDao().upsert(
            LocationFtsEntity(
                rowid = rowId,
                name = name,
                normalizedName = normalizedName,
            ),
        )
    }

    companion object {
        fun userDedupKey(id: String): String = "user:$id"
    }
}
