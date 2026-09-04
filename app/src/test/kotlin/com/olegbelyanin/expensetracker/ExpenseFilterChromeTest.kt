package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.model.Period
import com.olegbelyanin.expensetracker.ui.expenses.ExpenseFilterChrome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ExpenseFilterChromeTest {
    private val today = LocalDate.of(2026, 9, 4)
    private val groceries = category(1, "Продукты")
    private val cafe = category(2, "Кафе")
    private val health = category(3, "Здоровье")
    private val transport = category(4, "Транспорт")
    private val home = location(10, "Пятёрочка")

    @Test
    fun defaultShowsSingleAllChip() {
        assertEquals(listOf("Все"), chips(ExpenseListFilter()))
        assertEquals(
            listOf("Все"),
            chips(ExpenseListFilter(query = "латте", preset = ExpensePeriodPreset.ALL)),
        )
    }

    @Test
    fun chipsMirrorAppliedConstraints() {
        assertEquals(
            listOf("Сен"),
            chips(ExpenseListFilter(preset = ExpensePeriodPreset.CURRENT_MONTH)),
        )
        assertEquals(
            listOf("Сен", "Кафе"),
            chips(ExpenseListFilter(preset = ExpensePeriodPreset.CURRENT_MONTH, categoryIds = setOf(2))),
        )
        assertEquals(
            listOf("2 категории", "Пятёрочка"),
            chips(ExpenseListFilter(categoryIds = setOf(1, 2), locationId = 10)),
        )
        assertEquals(
            listOf("Авг–Сен"),
            chips(
                ExpenseListFilter(
                    preset = ExpensePeriodPreset.CUSTOM,
                    customPeriod = Period(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 4)),
                ),
            ),
        )
    }

    @Test
    fun collapsedCategoriesPreferSelectedThenMoreChip() {
        val categories = listOf(groceries, cafe, health, transport)
        assertEquals(
            listOf(cafe, health),
            ExpenseFilterChrome.visibleCategories(categories, setOf(2, 3), expanded = false),
        )
        assertTrue(ExpenseFilterChrome.showsMoreCategories(categories, expanded = false))
        assertEquals(categories, ExpenseFilterChrome.visibleCategories(categories, setOf(2), expanded = true))
        assertFalse(ExpenseFilterChrome.showsMoreCategories(categories, expanded = true))
    }

    @Test
    fun resolveLocationKeepsSelectedOrMatchesName() {
        val places = listOf(home, location(11, "Магнит"))
        assertEquals(10L, ExpenseFilterChrome.resolveLocationId("пятёрочка", 10, places))
        assertEquals(11L, ExpenseFilterChrome.resolveLocationId("Магнит", null, places))
        assertEquals(null, ExpenseFilterChrome.resolveLocationId("неизвестно", 10, places))
        assertEquals(null, ExpenseFilterChrome.resolveLocationId("  ", 10, places))
    }

    private fun chips(filter: ExpenseListFilter): List<String> = ExpenseFilterChrome.statusChipLabels(
        filter = filter,
        categories = listOf(groceries, cafe, health, transport),
        locations = listOf(home),
        today = today,
        categoryFallback = "Категория",
        placeFallback = "Место",
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
        usageCount = 2,
        lastUsedAt = Instant.parse("2026-09-02T00:00:00Z"),
        archivedAt = null,
    )
}
