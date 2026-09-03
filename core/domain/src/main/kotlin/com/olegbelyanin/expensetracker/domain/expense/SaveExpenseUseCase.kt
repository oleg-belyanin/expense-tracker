package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.PersistExpenseRequest
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class SaveExpenseUseCase(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val validator: ExpenseInputValidator = ExpenseInputValidator(),
    private val clock: Clock,
    private val zoneId: ZoneId,
) {
    suspend operator fun invoke(command: SaveExpenseCommand): SaveExpenseResult {
        val today = LocalDate.now(clock.withZone(zoneId))
        val fieldErrors = validator.validate(command, today)
        val categoryError = categoryError(command)
        val errors = fieldErrors.copy(category = categoryError)
        if (errors.hasErrors) {
            return SaveExpenseResult.Invalid(errors)
        }
        val amount = (validator.parseAmount(command.amountInput) as AmountParseResult.Valid).money
        val spentDate = command.spentAt ?: today
        val persisted = expenses.persist(
            PersistExpenseRequest(
                id = command.id ?: UUID.randomUUID().toString(),
                amount = amount,
                spentAt = spentDate.atStartOfDay(zoneId).toInstant(),
                name = command.name.trim(),
                categoryId = command.categoryId,
                locationName = command.locationName?.trim()?.ifEmpty { null },
                comment = command.comment?.trim()?.ifEmpty { null },
                categoryAssignmentSource = command.categoryAssignmentSource,
                proposedCategoryId = command.proposedCategoryId,
                interactive = command.interactive,
            ),
        )
        return SaveExpenseResult.Success(persisted)
    }

    private suspend fun categoryError(command: SaveExpenseCommand): CategoryFieldError? {
        val category = categories.findById(command.categoryId) ?: return CategoryFieldError.MISSING
        if (category.isActive) return null
        val existing = command.id?.let { expenses.get(it) }
        val keepingArchived = existing != null && existing.categoryId == command.categoryId
        return if (keepingArchived) null else CategoryFieldError.ARCHIVED
    }
}
