package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.ExpenseRepository

/**
 * F-11: удаляет историю расходов. Категории, места и правила категоризации остаются;
 * у `learning_example` обнуляется `expense_id` (§11.4 AD-CAT-001).
 */
class ClearExpenseHistoryUseCase(private val expenses: ExpenseRepository) {
    suspend operator fun invoke(): Int = expenses.clearHistory()
}
