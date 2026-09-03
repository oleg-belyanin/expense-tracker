package com.olegbelyanin.expensetracker.domain

import com.olegbelyanin.expensetracker.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun getActiveCategories(): List<Category>

    fun observeActiveCategories(): Flow<List<Category>>

    fun observeArchivedCategories(): Flow<List<Category>>

    fun observeAll(): Flow<List<Category>>

    suspend fun findById(id: Long): Category?

    suspend fun requireFallback(): Category

    suspend fun createUserCategory(name: String, color: String, icon: String): Category

    suspend fun updateUserCategory(id: Long, name: String, color: String, icon: String): Category

    suspend fun archive(id: Long): Category

    suspend fun restore(id: Long): Category
}
