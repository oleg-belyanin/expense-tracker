package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.model.ExpenseNameSuggestion

class SuggestExpenseNamesUseCase(private val expenses: ExpenseRepository) {
    suspend operator fun invoke(query: String, limit: Int = DEFAULT_LIMIT): List<ExpenseNameSuggestion> =
        expenses.suggestNames(query, limit)

    companion object {
        const val DEFAULT_LIMIT = 8
    }
}
