package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.model.ExpenseNameSuggestion

class SuggestExpenseNamesUseCase(private val expenses: ExpenseRepository) {
    suspend operator fun invoke(query: String, limit: Int = DEFAULT_LIMIT): List<ExpenseNameSuggestion> =
        expenses.suggestNames(query, limit)

    companion object {
        const val DEFAULT_LIMIT = 8
        const val EMPTY_FOCUS_LIMIT = 3

        fun limitFor(query: String): Int = if (query.trim().isEmpty()) EMPTY_FOCUS_LIMIT else DEFAULT_LIMIT
    }
}
