package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.data.filters.ExpenseListFilterRecord
import com.olegbelyanin.expensetracker.data.filters.ExpenseListFilterStorage
import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Period
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExpenseListFilterStorageTest {
    @Test
    fun roundTripKeepsPeriodCategoriesAndPlace() {
        val filter = ExpenseListFilter(
            query = "латте",
            preset = ExpensePeriodPreset.CUSTOM,
            customPeriod = Period(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 4)),
            categoryIds = setOf(3, 1),
            locationId = 10,
        )
        val restored = ExpenseListFilterStorage.decode(ExpenseListFilterStorage.encode(filter))
        assertEquals(ExpensePeriodPreset.CUSTOM, restored.preset)
        assertEquals(filter.customPeriod, restored.customPeriod)
        assertEquals(setOf(1L, 3L), restored.categoryIds)
        assertEquals(10L, restored.locationId)
        assertEquals("", restored.query)
    }

    @Test
    fun emptyRecordIsAllTime() {
        val restored = ExpenseListFilterStorage.decode(ExpenseListFilterRecord())
        assertEquals(ExpenseListFilter(), restored)
    }

    @Test
    fun unknownPresetFallsBackToAll() {
        val restored = ExpenseListFilterStorage.decode(
            ExpenseListFilterRecord(preset = "NEXT_WEEK", categoryIds = "1,x,2"),
        )
        assertEquals(ExpensePeriodPreset.ALL, restored.preset)
        assertEquals(setOf(1L, 2L), restored.categoryIds)
    }
}
