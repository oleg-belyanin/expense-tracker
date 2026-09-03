package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.LocationRepository
import com.olegbelyanin.expensetracker.domain.PersistExpenseRequest
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource

data class RecalculateCategoriesResult(
    val updated: Int,
    val unchanged: Int,
    val skippedExplicit: Int,
)

/**
 * Фоновый пересчёт категорий движком.
 * Явный выбор пользователя не трогается. Обучение не пишется (§11.1 AD-CAT-001).
 */
class RecalculateCategoriesUseCase(
    private val expenses: ExpenseRepository,
    private val locations: LocationRepository,
    private val suggestCategory: SuggestCategoryUseCase,
) {
    suspend operator fun invoke(): RecalculateCategoriesResult {
        var updated = 0
        var unchanged = 0
        var skippedExplicit = 0
        for (expense in expenses.getAll()) {
            if (expense.categoryAssignmentSource == CategoryAssignmentSource.EXPLICIT) {
                skippedExplicit++
                continue
            }
            val locationName = expense.locationId?.let { locations.findById(it)?.name }
            val suggestion = suggestCategory(expense.name, locationName)
            if (suggestion.selectedCategoryId == expense.categoryId &&
                suggestion.source == expense.categoryAssignmentSource
            ) {
                unchanged++
                continue
            }
            expenses.persist(
                PersistExpenseRequest(
                    id = expense.id,
                    amount = expense.amount,
                    spentAt = expense.spentAt,
                    name = expense.name,
                    categoryId = suggestion.selectedCategoryId,
                    locationName = locationName,
                    comment = expense.comment,
                    categoryAssignmentSource = suggestion.source,
                    proposedCategoryId = suggestion.selectedCategoryId,
                    interactive = false,
                    dedupKey = expense.dedupKey,
                ),
            )
            updated++
        }
        return RecalculateCategoriesResult(updated, unchanged, skippedExplicit)
    }
}
