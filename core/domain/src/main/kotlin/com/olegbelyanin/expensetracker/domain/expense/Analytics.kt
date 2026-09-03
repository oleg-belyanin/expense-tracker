package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.Period

data class AnalyticsCategoryRow(
    val categoryId: Long,
    val name: String,
    val icon: String,
    val color: String,
    val isBuiltin: Boolean,
    val amountMinor: Long,
    val share: Double,
    val sharePercent: Int,
)

data class AnalyticsSlice(
    val preset: ExpensePeriodPreset,
    val period: Period?,
    val totalMinor: Long,
    val rows: List<AnalyticsCategoryRow>,
) {
    val isEmpty: Boolean
        get() = rows.isEmpty()
}
