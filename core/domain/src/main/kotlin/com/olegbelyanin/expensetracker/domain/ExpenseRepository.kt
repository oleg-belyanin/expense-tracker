package com.olegbelyanin.expensetracker.domain

import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.Money
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class PersistExpenseRequest(
    val id: String,
    val amount: Money,
    val spentAt: Instant,
    val name: String,
    val categoryId: Long,
    val locationName: String?,
    val comment: String?,
    val categoryAssignmentSource: CategoryAssignmentSource,
    val proposedCategoryId: Long?,
    val interactive: Boolean = true,
)

interface ExpenseRepository {
    suspend fun get(id: String): Expense?

    fun observeAll(): Flow<List<Expense>>

    suspend fun persist(request: PersistExpenseRequest): Expense

    suspend fun delete(id: String)
}
