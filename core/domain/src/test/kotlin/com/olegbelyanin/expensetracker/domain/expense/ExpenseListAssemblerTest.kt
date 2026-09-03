package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.model.Money
import com.olegbelyanin.expensetracker.model.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ExpenseListAssemblerTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 9, 3)
    private val groceries = category(1, "Продукты")
    private val cafe = category(2, "Кафе")
    private val home = location(10, "Магазин у дома")

    @Test
    fun groupsByDateNewestFirstWithRelativeDays() {
        val slice = ExpenseListAssembler.build(
            expenses = listOf(
                expense("old", groceries.id, today.minusDays(3), "Хлеб", 1_000),
                expense("yesterday", cafe.id, today.minusDays(1), "Латте", 5_000),
                expense("today-a", groceries.id, today, "Молоко", 2_000, home.id),
                expense("today-b", cafe.id, today, "Капучино", 3_000),
            ),
            categories = listOf(groceries, cafe),
            locations = listOf(home),
            filter = ExpenseListFilter(),
            today = today,
            zoneId = zone,
        )
        assertEquals(3, slice.groups.size)
        assertEquals(DayRelative.TODAY, slice.groups[0].relative)
        assertEquals(5_000, slice.groups[0].totalMinor)
        assertEquals(listOf("Молоко", "Капучино"), slice.groups[0].items.map { it.name })
        assertEquals(DayRelative.YESTERDAY, slice.groups[1].relative)
        assertEquals(DayRelative.OTHER, slice.groups[2].relative)
        assertEquals(11_000, slice.totalMinor)
        assertEquals(4, slice.matchedCount)
    }

    @Test
    fun searchMatchesNamePlaceCommentAndCategory() {
        val expenses = listOf(
            expense("1", groceries.id, today, "Молоко", 1_000, home.id, "для каши"),
            expense("2", cafe.id, today, "Латте", 2_000),
        )
        assertEquals(
            listOf("Молоко"),
            names(expenses, ExpenseListFilter(query = "мага")),
        )
        assertEquals(
            listOf("Молоко"),
            names(expenses, ExpenseListFilter(query = "КАШИ")),
        )
        assertEquals(
            listOf("Латте"),
            names(expenses, ExpenseListFilter(query = "кафе")),
        )
    }

    @Test
    fun locationFilterCombinesWithPeriodAndCategory() {
        val park = location(11, "Парк")
        val expenses = listOf(
            expense("home-food", groceries.id, today, "Хлеб", 1_000, home.id),
            expense("park-food", groceries.id, today, "Пикник", 3_000, park.id),
            expense("home-aug", groceries.id, LocalDate.of(2026, 8, 15), "Арбуз", 4_000, home.id),
            expense("cafe", cafe.id, today, "Латте", 2_000, home.id),
        )
        val slice = ExpenseListAssembler.build(
            expenses = expenses,
            categories = listOf(groceries, cafe),
            locations = listOf(home, park),
            filter = ExpenseListFilter(
                preset = ExpensePeriodPreset.CURRENT_MONTH,
                categoryIds = setOf(groceries.id),
                locationId = home.id,
            ),
            today = today,
            zoneId = zone,
        )
        assertEquals(listOf("Хлеб"), slice.groups.flatMap { it.items }.map { it.name })
        assertEquals(1_000, slice.totalMinor)
        assertEquals(home.id, slice.groups.first().items.first().locationId)
    }

    @Test
    fun currentMonthAndCategoryFiltersCombine() {
        val expenses = listOf(
            expense("aug", groceries.id, LocalDate.of(2026, 8, 31), "Арбуз", 4_000),
            expense("sep-food", groceries.id, today, "Хлеб", 1_000),
            expense("sep-cafe", cafe.id, today, "Латте", 2_000),
        )
        val slice = ExpenseListAssembler.build(
            expenses = expenses,
            categories = listOf(groceries, cafe),
            locations = emptyList(),
            filter = ExpenseListFilter(
                preset = ExpensePeriodPreset.CURRENT_MONTH,
                categoryIds = setOf(groceries.id),
            ),
            today = today,
            zoneId = zone,
        )
        assertEquals(listOf("Хлеб"), slice.groups.flatMap { it.items }.map { it.name })
        assertEquals(1_000, slice.totalMinor)
        assertEquals(3, slice.storedCount)
        assertTrue(!slice.isFilterEmpty)
    }

    @Test
    fun previousMonthAndInvertedCustomRangeMatchAnalytics() {
        val expenses = listOf(
            expense("aug", groceries.id, LocalDate.of(2026, 8, 15), "Арбуз", 4_000),
            expense("sep", cafe.id, today, "Латте", 2_000),
        )
        val previous = ExpenseListAssembler.build(
            expenses = expenses,
            categories = listOf(groceries, cafe),
            locations = emptyList(),
            filter = ExpenseListFilter(preset = ExpensePeriodPreset.PREVIOUS_MONTH),
            today = today,
            zoneId = zone,
        )
        val custom = ExpenseListAssembler.build(
            expenses = expenses,
            categories = listOf(groceries, cafe),
            locations = emptyList(),
            filter = ExpenseListFilter(
                preset = ExpensePeriodPreset.CUSTOM,
                customPeriod = Period.of(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 1)),
                categoryIds = setOf(cafe.id),
            ),
            today = today,
            zoneId = zone,
        )
        assertEquals(4_000, previous.totalMinor)
        assertEquals(2_000, custom.totalMinor)
        assertEquals(listOf("Латте"), custom.groups.flatMap { it.items }.map { it.name })
    }

    @Test
    fun emptyFilterSliceIsDistinguishableFromEmptyDatabase() {
        val emptyDb = ExpenseListAssembler.build(
            expenses = emptyList(),
            categories = listOf(groceries),
            locations = emptyList(),
            filter = ExpenseListFilter(query = "нет"),
            today = today,
            zoneId = zone,
        )
        val noHits = ExpenseListAssembler.build(
            expenses = listOf(expense("1", groceries.id, today, "Хлеб", 1_000)),
            categories = listOf(groceries),
            locations = emptyList(),
            filter = ExpenseListFilter(query = "латте"),
            today = today,
            zoneId = zone,
        )
        assertTrue(emptyDb.isDatabaseEmpty)
        assertTrue(noHits.isFilterEmpty)
        assertEquals(0, noHits.totalMinor)
    }

    @Test
    fun storedCountCanComeFromRepositoryTotal() {
        val slice = ExpenseListAssembler.build(
            expenses = listOf(expense("1", groceries.id, today, "Хлеб", 1_000)),
            categories = listOf(groceries),
            locations = emptyList(),
            filter = ExpenseListFilter(query = "латте"),
            today = today,
            zoneId = zone,
            storedCount = 5_000,
        )
        assertEquals(5_000, slice.storedCount)
        assertTrue(slice.isFilterEmpty)
        assertEquals(0, slice.matchedCount)
    }

    private fun names(expenses: List<Expense>, filter: ExpenseListFilter): List<String> = ExpenseListAssembler.build(
        expenses = expenses,
        categories = listOf(groceries, cafe),
        locations = listOf(home),
        filter = filter,
        today = today,
        zoneId = zone,
    ).groups.flatMap { group -> group.items.map { it.name } }

    private fun expense(
        id: String,
        categoryId: Long,
        date: LocalDate,
        name: String,
        minor: Long,
        locationId: Long? = null,
        comment: String? = null,
    ) = Expense(
        id = id,
        amount = Money(minor),
        spentAt = date.atStartOfDay(zone).toInstant(),
        name = name,
        normalizedName = name.lowercase(),
        categoryId = categoryId,
        locationId = locationId,
        comment = comment,
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

    private fun location(id: Long, name: String) = Location(
        id = id,
        name = name,
        normalizedName = name.lowercase(),
        usageCount = 3,
        lastUsedAt = Instant.parse("2026-09-02T00:00:00Z"),
        archivedAt = null,
    )
}
