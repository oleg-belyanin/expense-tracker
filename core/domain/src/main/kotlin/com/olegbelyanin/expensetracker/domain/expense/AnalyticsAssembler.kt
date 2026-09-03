package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Expense
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor

object AnalyticsAssembler {
    fun build(
        expenses: List<Expense>,
        categories: List<Category>,
        filter: ExpenseListFilter,
        today: LocalDate,
        zoneId: ZoneId,
    ): AnalyticsSlice {
        val categoriesById = categories.associateBy { it.id }
        val period = ExpensePeriodResolver.resolve(filter.preset, filter.customPeriod, today)
        val totals = linkedMapOf<Long, Long>()
        expenses.forEach { expense ->
            if (categoriesById[expense.categoryId] == null) return@forEach
            val spentOn = expense.spentAt.atZone(zoneId).toLocalDate()
            if (period != null &&
                (spentOn.isBefore(period.startInclusive) || spentOn.isAfter(period.endInclusive))
            ) {
                return@forEach
            }
            totals[expense.categoryId] = (totals[expense.categoryId] ?: 0L) + expense.amount.minor
        }
        val ordered = totals.entries
            .filter { it.value > 0L }
            .sortedWith(
                compareByDescending<Map.Entry<Long, Long>> { it.value }
                    .thenBy { categoriesById.getValue(it.key).name.lowercase() },
            )
        val amounts = ordered.map { it.value }
        val total = amounts.sum()
        val percents = sharePercents(amounts)
        val rows = ordered.mapIndexed { index, (categoryId, amount) ->
            val category = categoriesById.getValue(categoryId)
            AnalyticsCategoryRow(
                categoryId = categoryId,
                name = category.name,
                icon = category.icon,
                color = category.color,
                isBuiltin = category.isBuiltin,
                amountMinor = amount,
                share = if (total == 0L) 0.0 else amount.toDouble() / total,
                sharePercent = percents[index],
            )
        }
        return AnalyticsSlice(
            preset = filter.preset,
            period = period,
            totalMinor = total,
            rows = rows,
        )
    }

    internal fun sharePercents(amounts: List<Long>): List<Int> {
        if (amounts.isEmpty()) return emptyList()
        val total = amounts.sum()
        if (total <= 0L) return List(amounts.size) { 0 }
        val exact = amounts.map { it * 100.0 / total }
        val floors = exact.map { floor(it).toInt() }.toMutableList()
        var leftover = 100 - floors.sum()
        val order = exact.indices.sortedWith(
            compareByDescending<Int> { exact[it] - floors[it] }.thenBy { it },
        )
        var cursor = 0
        while (leftover > 0 && order.isNotEmpty()) {
            floors[order[cursor % order.size]] += 1
            leftover -= 1
            cursor += 1
        }
        return floors
    }
}
