package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ExpensePeriodResolverTest {
    private val today = LocalDate.of(2026, 9, 3)

    @Test
    fun presetsResolveToClosedRanges() {
        assertNull(ExpensePeriodResolver.resolve(ExpensePeriodPreset.ALL, null, today))
        assertEquals(
            Period(LocalDate.of(2026, 9, 1), today),
            ExpensePeriodResolver.resolve(ExpensePeriodPreset.CURRENT_MONTH, null, today),
        )
        assertEquals(
            Period(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)),
            ExpensePeriodResolver.resolve(ExpensePeriodPreset.PREVIOUS_MONTH, null, today),
        )
        assertEquals(
            Period(LocalDate.of(2026, 1, 1), today),
            ExpensePeriodResolver.resolve(ExpensePeriodPreset.YEAR, null, today),
        )
    }

    @Test
    fun customRangeNormalizesInvertedDates() {
        val inverted = Period.of(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 8, 1))
        assertEquals(
            Period(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 2)),
            ExpensePeriodResolver.resolve(ExpensePeriodPreset.CUSTOM, inverted, today),
        )
    }

    @Test
    fun monthSelectionMapsNeighborsToPresets() {
        assertEquals(
            ExpensePeriodPreset.CURRENT_MONTH to null,
            ExpensePeriodResolver.monthSelection(YearMonth.of(2026, 9), today),
        )
        assertEquals(
            ExpensePeriodPreset.PREVIOUS_MONTH to null,
            ExpensePeriodResolver.monthSelection(YearMonth.of(2026, 8), today),
        )
        assertEquals(
            ExpensePeriodPreset.CUSTOM to Period(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)),
            ExpensePeriodResolver.monthSelection(YearMonth.of(2026, 7), today),
        )
    }
}
