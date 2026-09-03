package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.Period
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object ExpensePeriodResolver {
    fun resolve(preset: ExpensePeriodPreset, custom: Period?, today: LocalDate): Period? = when (preset) {
        ExpensePeriodPreset.ALL -> null

        ExpensePeriodPreset.CURRENT_MONTH -> Period(today.withDayOfMonth(1), today)

        ExpensePeriodPreset.PREVIOUS_MONTH -> {
            val month = YearMonth.from(today).minusMonths(1)
            Period(month.atDay(1), month.atEndOfMonth())
        }

        ExpensePeriodPreset.YEAR -> Period(today.withDayOfYear(1), today)

        ExpensePeriodPreset.CUSTOM ->
            custom?.let { Period.of(it.startInclusive, it.endInclusive) }
                ?: Period(today.withDayOfMonth(1), today)
    }

    fun monthSelection(month: YearMonth, today: LocalDate): Pair<ExpensePeriodPreset, Period?> {
        val current = YearMonth.from(today)
        return when {
            month.isAfter(current) -> ExpensePeriodPreset.CURRENT_MONTH to null

            month == current -> ExpensePeriodPreset.CURRENT_MONTH to null

            month == current.minusMonths(1) -> ExpensePeriodPreset.PREVIOUS_MONTH to null

            else -> {
                val end = month.atEndOfMonth().coerceAtMost(today)
                ExpensePeriodPreset.CUSTOM to Period(month.atDay(1), end)
            }
        }
    }

    fun defaultCustom(today: LocalDate): Period {
        val start = YearMonth.from(today).minusMonths(1).atDay(1)
        return Period(start, today)
    }

    fun toEpochRange(period: Period, zoneId: ZoneId): EpochRange {
        val start = period.startInclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endExclusive = period.endInclusive.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return EpochRange(startInclusive = start, endExclusive = endExclusive)
    }
}

data class EpochRange(val startInclusive: Long, val endExclusive: Long)
