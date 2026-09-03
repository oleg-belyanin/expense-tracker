package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.ui.analytics.AnalyticsSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class AnalyticsSelectionTest {
    private val today = LocalDate.of(2026, 9, 3)

    @Test
    fun arrowsMoveBetweenNeighborMonths() {
        val current = AnalyticsSelection()
        assertFalse(current.canGoNext(today))
        val previous = current.previous(today)
        assertEquals(ExpensePeriodPreset.PREVIOUS_MONTH, previous.preset)
        assertTrue(previous.canGoNext(today))
        assertEquals(ExpensePeriodPreset.CURRENT_MONTH, previous.next(today).preset)
        val july = previous.previous(today)
        assertEquals(ExpensePeriodPreset.CUSTOM, july.preset)
        assertEquals(YearMonth.of(2026, 7), july.anchorMonth(today))
    }

    @Test
    fun yearAndAllJumpToPreviousMonth() {
        val year = AnalyticsSelection(ExpensePeriodPreset.YEAR).previous(today)
        assertEquals(ExpensePeriodPreset.PREVIOUS_MONTH, year.preset)
        val all = AnalyticsSelection(ExpensePeriodPreset.ALL)
        assertFalse(all.canGoNext(today))
        assertEquals(ExpensePeriodPreset.PREVIOUS_MONTH, all.previous(today).preset)
    }

    @Test
    fun categoryFilterKeepsPeriod() {
        val selection = AnalyticsSelection(ExpensePeriodPreset.PREVIOUS_MONTH)
        val filter = selection.toCategoryFilter(7)
        assertEquals(ExpensePeriodPreset.PREVIOUS_MONTH, filter.preset)
        assertEquals(setOf(7L), filter.categoryIds)
    }
}
