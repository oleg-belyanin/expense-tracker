package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.Money
import com.olegbelyanin.expensetracker.model.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AnalyticsAssemblerTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 9, 3)
    private val groceries = category(1, "Продукты")
    private val cafe = category(2, "Кафе")
    private val transport = category(3, "Транспорт")

    @Test
    fun sumsSharesAndPercentsForCurrentMonth() {
        val slice = AnalyticsAssembler.build(
            expenses = listOf(
                expense("g1", groceries.id, today, 24_160_00),
                expense("c1", cafe.id, today, 9_664_00),
                expense("t1", transport.id, today, 7_248_00),
                expense("aug", groceries.id, LocalDate.of(2026, 8, 31), 50_000_00),
            ),
            categories = listOf(groceries, cafe, transport),
            filter = ExpenseListFilter(preset = ExpensePeriodPreset.CURRENT_MONTH),
            today = today,
            zoneId = zone,
        )
        assertEquals(41_072_00, slice.totalMinor)
        assertEquals(listOf("Продукты", "Кафе", "Транспорт"), slice.rows.map { it.name })
        assertEquals(listOf(59, 23, 18), slice.rows.map { it.sharePercent })
        assertEquals(100, slice.rows.sumOf { it.sharePercent })
        assertEquals(24_160_00, slice.rows.first().amountMinor)
    }

    @Test
    fun emptyPeriodHasZeroTotalAndNoRows() {
        val slice = AnalyticsAssembler.build(
            expenses = listOf(expense("aug", groceries.id, LocalDate.of(2026, 8, 1), 1_000)),
            categories = listOf(groceries),
            filter = ExpenseListFilter(preset = ExpensePeriodPreset.CURRENT_MONTH),
            today = today,
            zoneId = zone,
        )
        assertTrue(slice.isEmpty)
        assertEquals(0, slice.totalMinor)
        assertTrue(slice.rows.isEmpty())
        assertEquals(emptyList<Int>(), AnalyticsAssembler.sharePercents(emptyList()))
    }

    @Test
    fun previousMonthAndCustomRangeMatchListTotals() {
        val expenses = listOf(
            expense("aug", groceries.id, LocalDate.of(2026, 8, 15), 4_000),
            expense("sep", cafe.id, today, 2_000),
        )
        val previous = AnalyticsAssembler.build(
            expenses = expenses,
            categories = listOf(groceries, cafe),
            filter = ExpenseListFilter(preset = ExpensePeriodPreset.PREVIOUS_MONTH),
            today = today,
            zoneId = zone,
        )
        val custom = AnalyticsAssembler.build(
            expenses = expenses,
            categories = listOf(groceries, cafe),
            filter = ExpenseListFilter(
                preset = ExpensePeriodPreset.CUSTOM,
                customPeriod = Period.of(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 1)),
            ),
            today = today,
            zoneId = zone,
        )
        assertEquals(4_000, previous.totalMinor)
        assertEquals(listOf("Продукты"), previous.rows.map { it.name })
        assertEquals(2_000, custom.totalMinor)
        assertEquals(listOf("Кафе"), custom.rows.map { it.name })
    }

    @Test
    fun sharePercentsUseLargestRemainder() {
        assertEquals(listOf(34, 33, 33), AnalyticsAssembler.sharePercents(listOf(1, 1, 1)))
        assertEquals(listOf(50, 50), AnalyticsAssembler.sharePercents(listOf(5, 5)))
        assertEquals(listOf(0), AnalyticsAssembler.sharePercents(listOf(0)))
    }

    private fun expense(id: String, categoryId: Long, date: LocalDate, minor: Long) = Expense(
        id = id,
        amount = Money(minor),
        spentAt = date.atStartOfDay(zone).toInstant(),
        name = id,
        normalizedName = id,
        categoryId = categoryId,
        locationId = null,
        comment = null,
        categoryAssignmentSource = CategoryAssignmentSource.FALLBACK,
        dedupKey = "user:$id",
    )

    private fun category(id: Long, name: String) = Category(
        id = id,
        code = name.uppercase(),
        name = name,
        normalizedName = name.lowercase(),
        color = "#FFFFFF",
        icon = "other",
        isBuiltin = true,
        archivedAt = null,
    )
}
