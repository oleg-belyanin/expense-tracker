package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.PersistExpenseRequest
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.Money
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class ImportExpenseDraft(
    val id: String? = null,
    val amount: Money,
    val spentAt: Instant,
    val name: String,
    val locationName: String? = null,
    val comment: String? = null,
    val dedupKey: String? = null,
)

data class ImportExpensesResult(val imported: List<Expense>, val skippedDuplicate: Int, val skippedInvalid: Int)

/**
 * Пакетная вставка расходов с автокатегоризацией.
 * Не интерактивное согласие: `interactive=false`, обучение не пишется (§11.1 AD-CAT-001).
 */
class ImportExpensesUseCase(
    private val expenses: ExpenseRepository,
    private val suggestCategory: SuggestCategoryUseCase,
    private val clock: Clock,
    private val zoneId: ZoneId,
) {
    suspend operator fun invoke(drafts: List<ImportExpenseDraft>): ImportExpensesResult {
        val today = LocalDate.now(clock.withZone(zoneId))
        val imported = mutableListOf<Expense>()
        var skippedDuplicate = 0
        var skippedInvalid = 0
        for (draft in drafts) {
            val name = draft.name.trim()
            val spentDate = draft.spentAt.atZone(zoneId).toLocalDate()
            if (name.isEmpty() || spentDate.isAfter(today)) {
                skippedInvalid++
                continue
            }
            val id = draft.id ?: UUID.randomUUID().toString()
            val locationName = draft.locationName?.trim()?.ifEmpty { null }
            val comment = draft.comment?.trim()?.ifEmpty { null }
            val dedupKey = draft.dedupKey ?: contentDedupKey(name, draft.spentAt, draft.amount, locationName)
            if (expenses.get(id) != null || expenses.findByDedupKey(dedupKey) != null) {
                skippedDuplicate++
                continue
            }
            val suggestion = suggestCategory(name, locationName)
            imported += expenses.persist(
                PersistExpenseRequest(
                    id = id,
                    amount = draft.amount,
                    spentAt = draft.spentAt,
                    name = name,
                    categoryId = suggestion.selectedCategoryId,
                    locationName = locationName,
                    comment = comment,
                    categoryAssignmentSource = suggestion.source,
                    proposedCategoryId = suggestion.selectedCategoryId,
                    interactive = false,
                    dedupKey = dedupKey,
                ),
            )
        }
        return ImportExpensesResult(imported, skippedDuplicate, skippedInvalid)
    }

    companion object {
        fun contentDedupKey(name: String, spentAt: Instant, amount: Money, locationName: String?): String {
            val location = locationName?.trim().orEmpty().lowercase()
            return "import:${name.trim().lowercase()}|${spentAt.toEpochMilli()}|${amount.minor}|$location"
        }
    }
}
