package com.olegbelyanin.expensetracker.domain.backup

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.LocationRepository
import kotlinx.coroutines.flow.first

class ExportExpensesCsvUseCase(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val locations: LocationRepository,
) {
    suspend operator fun invoke(): String {
        val categoryById = categories.observeAll().first().associateBy { it.id }
        val locationById = locations.observeAll().first().associateBy { it.id }
        val rows = expenses.getAll().map { expense ->
            val category = categoryById[expense.categoryId]
            ExpenseCsv.Row(
                id = expense.id,
                spentAt = expense.spentAt,
                amountMinor = expense.amount.minor,
                name = expense.name,
                categoryName = category?.name.orEmpty(),
                categoryCode = category?.code,
                locationName = expense.locationId?.let { locationById[it]?.name },
                comment = expense.comment,
                assignmentSource = expense.categoryAssignmentSource.name.lowercase(),
                dedupKey = expense.dedupKey,
            )
        }
        return ExpenseCsv.write(rows)
    }
}
