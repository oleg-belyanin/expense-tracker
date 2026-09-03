package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.domain.expense.DayRelative
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
}
