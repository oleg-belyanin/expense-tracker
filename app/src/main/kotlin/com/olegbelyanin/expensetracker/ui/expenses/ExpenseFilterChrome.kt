package com.olegbelyanin.expensetracker.ui.expenses

import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Location

object ExpenseFilterChrome {
    const val COLLAPSED_CATEGORY_LIMIT = 2

    fun isCompact(filter: ExpenseListFilter): Boolean = filter.categoryIds.isNotEmpty() ||
        filter.locationId != null ||
        filter.preset == ExpensePeriodPreset.YEAR ||
        filter.preset == ExpensePeriodPreset.CUSTOM ||
        filter.preset == ExpensePeriodPreset.PREVIOUS_MONTH

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
}
