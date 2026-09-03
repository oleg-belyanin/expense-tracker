package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ExpenseSearchQueryTest {
    private val today = LocalDate.of(2026, 9, 3)
    private val zone = ZoneOffset.UTC

    @Test
    fun allTimeHasOpenSpentAtBounds() {
        val query = ExpenseSearchQuery.from(ExpenseListFilter(), today, zone)
        assertNull(query.spentAtFromInclusive)
        assertNull(query.spentAtToExclusive)
        assertEquals(emptySet<Long>(), query.categoryIds)
        assertNull(query.locationId)
    }

    @Test
    fun invertedCustomRangeAndPlaceBecomeSqlBounds() {
        val query = ExpenseSearchQuery.from(
            ExpenseListFilter(
                preset = ExpensePeriodPreset.CUSTOM,
                customPeriod = Period.of(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 1)),
                categoryIds = setOf(2L),
                locationId = 10L,
                query = "латте",
            ),
            today,
            zone,
        )
        val expected = ExpensePeriodResolver.toEpochRange(
            Period(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3)),
            zone,
        )
        assertEquals(expected.startInclusive, query.spentAtFromInclusive)
        assertEquals(expected.endExclusive, query.spentAtToExclusive)
        assertEquals(setOf(2L), query.categoryIds)
        assertEquals(10L, query.locationId)
        assertEquals("латте", query.text)
    }

    @Test
    fun analyticsPeriodOnlyDropsSearchAndCategory() {
        val source = ExpenseListFilter(
            query = "хлеб",
            preset = ExpensePeriodPreset.PREVIOUS_MONTH,
            categoryIds = setOf(1L),
            locationId = 4L,
        )
        val periodOnly = ExpenseListFilter(preset = source.preset, customPeriod = source.customPeriod)
        val query = ExpenseSearchQuery.from(periodOnly, today, zone)
        assertEquals(emptySet<Long>(), query.categoryIds)
        assertNull(query.locationId)
        assertEquals("", query.text)
        val previous = ExpensePeriodResolver.toEpochRange(
            Period(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)),
            zone,
        )
        assertEquals(previous.startInclusive, query.spentAtFromInclusive)
        assertEquals(previous.endExclusive, query.spentAtToExclusive)
    }
}
