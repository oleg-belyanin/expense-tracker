package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.Period
import java.time.Instant
import java.time.LocalDate

enum class ExpensePeriodPreset {
    ALL,
    CURRENT_MONTH,
    PREVIOUS_MONTH,
    YEAR,
    CUSTOM,
}

data class ExpenseListFilter(
    val query: String = "",
    val preset: ExpensePeriodPreset = ExpensePeriodPreset.ALL,
    val customPeriod: Period? = null,
    val categoryIds: Set<Long> = emptySet(),
) {
    val hasActiveConstraints: Boolean
        get() = query.isNotBlank() ||
            preset != ExpensePeriodPreset.ALL ||
            customPeriod != null ||
            categoryIds.isNotEmpty()
}

enum class DayRelative {
    TODAY,
    YESTERDAY,
    OTHER,
}

data class ExpenseListItem(
    val id: String,
    val name: String,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val locationName: String?,
    val comment: String?,
    val amountMinor: Long,
    val spentAt: Instant,
    val spentOn: LocalDate,
)

data class ExpenseDayGroup(
    val date: LocalDate,
    val relative: DayRelative,
    val totalMinor: Long,
    val items: List<ExpenseListItem>,
)

data class ExpenseListSlice(
    val groups: List<ExpenseDayGroup>,
    val totalMinor: Long,
    val matchedCount: Int,
    val storedCount: Int,
) {
    val isDatabaseEmpty: Boolean
        get() = storedCount == 0

    val isFilterEmpty: Boolean
        get() = storedCount > 0 && matchedCount == 0
}
