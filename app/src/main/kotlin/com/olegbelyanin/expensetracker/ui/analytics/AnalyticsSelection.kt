package com.olegbelyanin.expensetracker.ui.analytics

import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodResolver
import com.olegbelyanin.expensetracker.model.Period
import java.time.LocalDate
import java.time.YearMonth

data class AnalyticsSelection(
    val preset: ExpensePeriodPreset = ExpensePeriodPreset.CURRENT_MONTH,
    val customPeriod: Period? = null,
) {
    fun toFilter(): ExpenseListFilter = ExpenseListFilter(preset = preset, customPeriod = customPeriod)

    fun toCategoryFilter(categoryId: Long): ExpenseListFilter = ExpenseListFilter(
        preset = preset,
        customPeriod = customPeriod,
        categoryIds = setOf(categoryId),
    )

    fun previous(today: LocalDate): AnalyticsSelection {
        if (preset == ExpensePeriodPreset.YEAR || preset == ExpensePeriodPreset.ALL) {
            return ofMonth(YearMonth.from(today).minusMonths(1), today)
        }
        return shiftMonth(-1, today)
    }

    fun next(today: LocalDate): AnalyticsSelection = if (canGoNext(today)) shiftMonth(1, today) else this

    fun canGoNext(today: LocalDate): Boolean {
        if (preset == ExpensePeriodPreset.YEAR || preset == ExpensePeriodPreset.ALL) {
            return false
        }
        return anchorMonth(today).isBefore(YearMonth.from(today))
    }

    fun shiftMonth(delta: Long, today: LocalDate): AnalyticsSelection =
        ofMonth(anchorMonth(today).plusMonths(delta), today)

    fun anchorMonth(today: LocalDate): YearMonth = when (preset) {
        ExpensePeriodPreset.CURRENT_MONTH,
        ExpensePeriodPreset.YEAR,
        ExpensePeriodPreset.ALL,
        -> YearMonth.from(today)

        ExpensePeriodPreset.PREVIOUS_MONTH -> YearMonth.from(today).minusMonths(1)

        ExpensePeriodPreset.CUSTOM ->
            customPeriod?.let { YearMonth.from(it.endInclusive) } ?: YearMonth.from(today)
    }

    companion object {
        fun ofMonth(month: YearMonth, today: LocalDate): AnalyticsSelection {
            val (preset, custom) = ExpensePeriodResolver.monthSelection(month, today)
            return AnalyticsSelection(preset, custom)
        }
    }
}
