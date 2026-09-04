package com.olegbelyanin.expensetracker.ui.expenses

import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import java.time.LocalDate

object ExpenseFilterChrome {
    const val COLLAPSED_CATEGORY_LIMIT = 2

    fun statusChipLabels(
        filter: ExpenseListFilter,
        categories: List<Category>,
        locations: List<Location>,
        today: LocalDate,
        categoryFallback: String,
        placeFallback: String,
    ): List<String> {
        if (!hasVisualConstraints(filter)) {
            return listOf(ExpenseFormat.periodChip(ExpensePeriodPreset.ALL, today))
        }
        val chips = mutableListOf<String>()
        if (filter.preset != ExpensePeriodPreset.ALL) {
            chips += ExpenseFormat.periodChip(filter.preset, today, filter.customPeriod)
        }
        if (filter.categoryIds.isNotEmpty()) {
            chips +=
                if (filter.categoryIds.size == 1) {
                    categories.firstOrNull { it.id == filter.categoryIds.first() }?.name ?: categoryFallback
                } else {
                    ExpenseFormat.categoryCount(filter.categoryIds.size)
                }
        }
        filter.locationId?.let { id ->
            chips += locations.firstOrNull { it.id == id }?.name ?: placeFallback
        }
        return chips
    }

    fun visibleCategories(categories: List<Category>, selectedIds: Set<Long>, expanded: Boolean): List<Category> {
        if (expanded || !showsMoreCategories(categories, expanded = false)) {
            return categories
        }
        val selected = categories.filter { it.id in selectedIds }
        val rest = categories.filter { it.id !in selectedIds }
        return (selected + rest).take(COLLAPSED_CATEGORY_LIMIT)
    }

    fun showsMoreCategories(categories: List<Category>, expanded: Boolean): Boolean =
        !expanded && categories.size > COLLAPSED_CATEGORY_LIMIT + 1

    fun resolveLocationId(name: String, selectedId: Long?, locations: List<Location>): Long? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        selectedId?.let { id ->
            if (locations.any { it.id == id && it.name.equals(trimmed, ignoreCase = true) }) {
                return id
            }
        }
        return locations.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.id
    }

    private fun hasVisualConstraints(filter: ExpenseListFilter): Boolean = filter.preset != ExpensePeriodPreset.ALL ||
        filter.categoryIds.isNotEmpty() ||
        filter.locationId != null
}
