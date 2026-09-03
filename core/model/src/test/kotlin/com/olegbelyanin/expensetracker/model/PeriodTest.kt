package com.olegbelyanin.expensetracker.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodTest {
    @Test
    fun invertedRangeIsNormalized() {
        val start = LocalDate.of(2026, 9, 2)
        val end = LocalDate.of(2026, 8, 1)
        val period = Period.of(start, end)
        assertEquals(end, period.startInclusive)
        assertEquals(start, period.endInclusive)
    }

    @Test
    fun orderedRangeStaysAsIs() {
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 9, 2)
        val period = Period.of(start, end)
        assertEquals(start, period.startInclusive)
        assertEquals(end, period.endInclusive)
    }
}
