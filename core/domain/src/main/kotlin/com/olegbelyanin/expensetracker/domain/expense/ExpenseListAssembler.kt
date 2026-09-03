package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.model.Period
import java.time.LocalDate
import java.time.ZoneId

object ExpenseListAssembler {
    fun build(
        expenses: List<Expense>,
        categories: List<Category>,
        locations: List<Location>,
        filter: ExpenseListFilter,
        today: LocalDate,
        zoneId: ZoneId,
        storedCount: Int? = null,
    ): ExpenseListSlice {
        val categoriesById = categories.associateBy { it.id }
        val locationsById = locations.associateBy { it.id }
        val period = ExpensePeriodResolver.resolve(filter.preset, filter.customPeriod, today)
        val items = expenses.mapNotNull { expense ->
            val category = categoriesById[expense.categoryId] ?: return@mapNotNull null
            val location = expense.locationId?.let { locationsById[it] }
            val spentOn = expense.spentAt.atZone(zoneId).toLocalDate()
            val item = ExpenseListItem(
                id = expense.id,
                name = expense.name,
                categoryId = category.id,
                categoryName = category.name,
                categoryIcon = category.icon,
                categoryColor = category.color,
                locationId = expense.locationId,
                locationName = location?.name,
                comment = expense.comment,
                amountMinor = expense.amount.minor,
                spentAt = expense.spentAt,
                spentOn = spentOn,
            )
            item.takeIf { matchesConstraints(it, filter, period) }
        }.sortedWith(compareByDescending<ExpenseListItem> { it.spentOn }.thenByDescending { it.spentAt })
        val groups = items
            .groupBy { it.spentOn }
            .toSortedMap(compareByDescending { it })
            .map { (date, dayItems) ->
                ExpenseDayGroup(
                    date = date,
                    relative = relative(date, today),
                    totalMinor = dayItems.sumOf { it.amountMinor },
                    items = dayItems,
                )
            }
        return ExpenseListSlice(
            groups = groups,
            totalMinor = items.sumOf { it.amountMinor },
            matchedCount = items.size,
            storedCount = storedCount ?: expenses.size,
        )
    }

    private fun matchesConstraints(item: ExpenseListItem, filter: ExpenseListFilter, period: Period?): Boolean {
        if (period != null &&
            (item.spentOn.isBefore(period.startInclusive) || item.spentOn.isAfter(period.endInclusive))
        ) {
            return false
        }
        if (filter.categoryIds.isNotEmpty() && item.categoryId !in filter.categoryIds) {
            return false
        }
        if (filter.locationId != null && item.locationId != filter.locationId) {
            return false
        }
        return true
    }

    private fun relative(date: LocalDate, today: LocalDate): DayRelative = when (date) {
        today -> DayRelative.TODAY
        today.minusDays(1) -> DayRelative.YESTERDAY
        else -> DayRelative.OTHER
    }
}
