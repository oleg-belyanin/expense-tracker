package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.Money
import java.time.LocalDate

enum class AmountFieldError {
    EMPTY,
    ZERO,
    NEGATIVE,
    NON_NUMERIC,
}

enum class DateFieldError {
    FUTURE,
}

enum class NameFieldError {
    EMPTY,
}

enum class CategoryFieldError {
    MISSING,
    ARCHIVED,
}

data class ExpenseValidationErrors(
    val amount: AmountFieldError? = null,
    val date: DateFieldError? = null,
    val name: NameFieldError? = null,
    val category: CategoryFieldError? = null,
) {
    val hasErrors: Boolean
        get() = amount != null || date != null || name != null || category != null
}

data class SaveExpenseCommand(
    val id: String? = null,
    val amountInput: String,
    val spentAt: LocalDate? = null,
    val name: String,
    val categoryId: Long,
    val locationName: String? = null,
    val comment: String? = null,
    val categoryAssignmentSource: CategoryAssignmentSource,
    val proposedCategoryId: Long? = null,
    val interactive: Boolean = true,
)

sealed interface AmountParseResult {
    data object Empty : AmountParseResult

    data object Zero : AmountParseResult

    data object Negative : AmountParseResult

    data object NonNumeric : AmountParseResult

    data class Valid(val money: Money) : AmountParseResult
}

sealed interface SaveExpenseResult {
    data class Success(val expense: Expense) : SaveExpenseResult

    data class Invalid(val errors: ExpenseValidationErrors) : SaveExpenseResult
}
