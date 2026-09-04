package com.olegbelyanin.expensetracker.data.filters

import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Period
import java.time.LocalDate

data class ExpenseListFilterRecord(
    val preset: String? = null,
    val customStart: String? = null,
    val customEnd: String? = null,
    val categoryIds: String? = null,
    val locationId: Long? = null,
)

object ExpenseListFilterStorage {
    fun encode(filter: ExpenseListFilter): ExpenseListFilterRecord {
        val custom = filter.customPeriod.takeIf { filter.preset == ExpensePeriodPreset.CUSTOM }
        return ExpenseListFilterRecord(
            preset = filter.preset.name,
            customStart = custom?.startInclusive?.toString(),
            customEnd = custom?.endInclusive?.toString(),
            categoryIds = filter.categoryIds.sorted().joinToString(","),
            locationId = filter.locationId,
        )
    }

    fun decode(record: ExpenseListFilterRecord): ExpenseListFilter {
        val preset = ExpensePeriodPreset.entries.firstOrNull { it.name == record.preset }
            ?: ExpensePeriodPreset.ALL
        val custom = period(record.customStart, record.customEnd)
            .takeIf { preset == ExpensePeriodPreset.CUSTOM }
        return ExpenseListFilter(
            preset = preset,
            customPeriod = custom,
            categoryIds = categoryIds(record.categoryIds),
            locationId = record.locationId,
        )
    }

    private fun period(start: String?, end: String?): Period? {
        val startDate = date(start) ?: return null
        val endDate = date(end) ?: return null
        return Period.of(startDate, endDate)
    }

    private fun date(value: String?): LocalDate? = value?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }

    private fun categoryIds(value: String?): Set<Long> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(',')
            .mapNotNull { part -> part.trim().toLongOrNull() }
            .toSet()
    }
}
