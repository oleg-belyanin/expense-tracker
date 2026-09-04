package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.ui.expenses.ExpenseFilterChrome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExpenseFilterChromeTest {
    private val groceries = category(1, "Продукты")
    private val cafe = category(2, "Кафе")
    private val health = category(3, "Здоровье")
    private val transport = category(4, "Транспорт")
    private val home = location(10, "Пятёрочка")

    @Test
    fun idleMonthStaysOnQuickChips() {
        assertFalse(ExpenseFilterChrome.isCompact(ExpenseListFilter()))
        assertFalse(
            ExpenseFilterChrome.isCompact(ExpenseListFilter(preset = ExpensePeriodPreset.CURRENT_MONTH)),
        )
    }

    @Test
    fun compactWhenPlaceCategoriesOrExtraPeriod() {
        assertTrue(ExpenseFilterChrome.isCompact(ExpenseListFilter(categoryIds = setOf(1))))
        assertTrue(ExpenseFilterChrome.isCompact(ExpenseListFilter(locationId = 10)))
        assertTrue(ExpenseFilterChrome.isCompact(ExpenseListFilter(preset = ExpensePeriodPreset.YEAR)))
        assertTrue(ExpenseFilterChrome.isCompact(ExpenseListFilter(preset = ExpensePeriodPreset.PREVIOUS_MONTH)))
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
