package com.olegbelyanin.expensetracker.database

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.model.BuiltinCategories
import com.olegbelyanin.expensetracker.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCategoryRepository(private val database: AppDatabase) : CategoryRepository {
    override suspend fun getActiveCategories(): List<Category> =
        database.categoryDao().getActive().map { it.toDomain() }

    override fun observeActiveCategories(): Flow<List<Category>> =
        database.categoryDao().observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: Long): Category? = database.categoryDao().findById(id)?.toDomain()

    override suspend fun requireFallback(): Category {
        val entity = database.categoryDao().findBuiltinByCode(BuiltinCategories.FALLBACK_CODE)
            ?: error("Fallback category ${BuiltinCategories.FALLBACK_CODE} is missing")
        return entity.toDomain()
    }
}
