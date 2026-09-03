package com.olegbelyanin.expensetracker.domain.expense

import java.time.LocalDate
import java.time.ZoneId

data class ExpenseSearchQuery(
    val spentAtFromInclusive: Long? = null,
    val spentAtToExclusive: Long? = null,
    val categoryIds: Set<Long> = emptySet(),
    val locationId: Long? = null,
    val text: String = "",
) {
    companion object {
        fun from(filter: ExpenseListFilter, today: LocalDate, zoneId: ZoneId): ExpenseSearchQuery {
            val period = ExpensePeriodResolver.resolve(filter.preset, filter.customPeriod, today)
            val range = period?.let { ExpensePeriodResolver.toEpochRange(it, zoneId) }
            return ExpenseSearchQuery(
                spentAtFromInclusive = range?.startInclusive,
                spentAtToExclusive = range?.endExclusive,
                categoryIds = filter.categoryIds,
                locationId = filter.locationId,
                text = filter.query,
            )
        }
    }
}
