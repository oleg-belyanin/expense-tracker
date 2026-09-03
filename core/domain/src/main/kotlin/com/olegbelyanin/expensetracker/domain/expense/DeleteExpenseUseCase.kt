package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.ExpenseRepository

class DeleteExpenseUseCase(private val expenses: ExpenseRepository) {
    suspend operator fun invoke(id: String) {
        expenses.delete(id)
    }
}
