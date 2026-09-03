package com.olegbelyanin.expensetracker.domain

import com.olegbelyanin.expensetracker.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun getActiveCategories(): List<Category>

    fun observeActiveCategories(): Flow<List<Category>>

    suspend fun findById(id: Long): Category?

    suspend fun requireFallback(): Category
}
