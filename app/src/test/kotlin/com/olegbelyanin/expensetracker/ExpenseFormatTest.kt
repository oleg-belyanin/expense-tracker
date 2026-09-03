package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.domain.expense.DayRelative
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Period
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ExpenseFormatTest {
    private val today = LocalDate.of(2026, 9, 3)

    @Test
    fun formatsRublesWithGroupingAndMinus() {
        assertEquals("0 ₽", ExpenseFormat.money(0))
        assertTrue(ExpenseFormat.money(184_000).endsWith("₽"))
        assertTrue(ExpenseFormat.money(184_000).contains("1"))
        assertTrue(ExpenseFormat.money(184_000, withMinus = true).startsWith("−"))
        assertEquals("1840,50", ExpenseFormat.moneyInput(184_050))
    }

    @Test
    fun periodAndDayLabels() {
        assertEquals("За всё время", ExpenseFormat.periodSubtitle(ExpensePeriodPreset.ALL, today))
        assertEquals("В сентябре", ExpenseFormat.periodSubtitle(ExpensePeriodPreset.CURRENT_MONTH, today))
        assertEquals("В августе", ExpenseFormat.periodSubtitle(ExpensePeriodPreset.PREVIOUS_MONTH, today))
        assertEquals("В 2026 году", ExpenseFormat.periodSubtitle(ExpensePeriodPreset.YEAR, today))
        assertEquals(
            "1 августа — 2 сентября",
            ExpenseFormat.periodSubtitle(
                ExpensePeriodPreset.CUSTOM,
                today,
                Period.of(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 2)),
            ),
        )
        assertEquals("Сентябрь 2026", ExpenseFormat.monthYear(YearMonth.of(2026, 9), capitalize = true))
        val share = ExpenseFormat.shareLine(2_416_000, 50)
        assertTrue(share.contains("24"))
        assertTrue(share.endsWith("50%"))
        assertTrue(
            ExpenseFormat.analyticsHeader(4_832_000, ExpensePeriodPreset.CURRENT_MONTH, today)
                .endsWith("сентябрь 2026"),
        )
        assertEquals("Сегодня", ExpenseFormat.dayHeader(today, DayRelative.TODAY))
        assertEquals("Сегодня, 3 сентября", ExpenseFormat.formDate(today, today))
        assertEquals("Вчера, 2 сентября", ExpenseFormat.formDate(today.minusDays(1), today))
    }

    @Test
    fun russianUsagePlural() {
        assertEquals("1 использование", ExpenseFormat.usages(1))
        assertEquals("2 использования", ExpenseFormat.usages(2))
        assertEquals("12 использований", ExpenseFormat.usages(12))
        assertEquals("21 использование", ExpenseFormat.usages(21))
    }

    @Test
    fun russianCategoryAndExpensePlurals() {
        assertEquals("1 категория", ExpenseFormat.categoryCount(1))
        assertEquals("2 категории", ExpenseFormat.categoryCount(2))
        assertEquals("12 категорий", ExpenseFormat.categoryCount(12))
        assertEquals("36 расходов", ExpenseFormat.expenseCount(36))
        assertEquals("сегодня", ExpenseFormat.archivedDay(today, today))
        assertEquals("1 сентября", ExpenseFormat.archivedDay(LocalDate.of(2026, 9, 1), today))
    }

    @Test
    fun scorePercentMatchesPickerCaption() {
        assertEquals("87%", ExpenseFormat.scorePercent(0.87))
        assertEquals("9%", ExpenseFormat.scorePercent(0.09))
        assertEquals("2%", ExpenseFormat.scorePercent(0.02))
    }
}
