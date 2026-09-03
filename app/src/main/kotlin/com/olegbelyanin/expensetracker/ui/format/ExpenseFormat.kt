package com.olegbelyanin.expensetracker.ui.format

import com.olegbelyanin.expensetracker.domain.expense.DayRelative
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Location
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

object ExpenseFormat {
    private val symbols = DecimalFormatSymbols(Locale("ru", "RU"))
    private val grouping = DecimalFormat("#,##0", symbols)

    fun money(minor: Long, withMinus: Boolean = false): String {
        val sign = if (withMinus && minor > 0) "−" else ""
        if (minor <= 0L) return "0 ₽"
        val major = minor / 100
        val fraction = (minor % 100).toInt()
        val grouped = grouping.format(major)
        val body = if (fraction == 0) "$grouped ₽" else "$grouped,${fraction.toString().padStart(2, '0')} ₽"
        return sign + body
    }

    fun moneyInput(minor: Long): String {
        val major = minor / 100
        val fraction = (minor % 100).toInt()
        return if (fraction == 0) major.toString() else "$major,${fraction.toString().padStart(2, '0')}"
    }

    fun periodSubtitle(preset: ExpensePeriodPreset, today: LocalDate): String = when (preset) {
        ExpensePeriodPreset.ALL -> "За всё время"
        ExpensePeriodPreset.CURRENT_MONTH -> "В ${MONTHS_PREPOSITIONAL[today.monthValue - 1]}"
    }

    fun dayHeader(date: LocalDate, relative: DayRelative): String = when (relative) {
        DayRelative.TODAY -> "Сегодня"
        DayRelative.YESTERDAY -> "Вчера"
        DayRelative.OTHER -> calendarDay(date)
    }

    fun formDate(date: LocalDate, today: LocalDate): String {
        val calendar = "${date.dayOfMonth} ${MONTHS_GENITIVE[date.monthValue - 1]}"
        return when (date) {
            today -> "Сегодня, $calendar"
            today.minusDays(1) -> "Вчера, $calendar"
            else -> if (date.year == today.year) calendar else "$calendar ${date.year}"
        }
    }

    fun calendarDay(date: LocalDate): String {
        val month = MONTHS_GENITIVE[date.monthValue - 1]
        return "${date.dayOfMonth} $month"
    }

    fun locationDetail(location: Location, today: LocalDate, zoneId: ZoneId): String {
        val lastUsed = location.lastUsedAt?.atZone(zoneId)?.toLocalDate()
        return when (lastUsed) {
            today -> "Последнее использование — сегодня"
            today.minusDays(1) -> "Последнее использование — вчера"
            else -> usages(location.usageCount)
        }
    }

    fun usages(count: Int): String {
        val n10 = count % 10
        val n100 = count % 100
        val word = when {
            n10 == 1 && n100 != 11 -> "использование"
            n10 in 2..4 && n100 !in 12..14 -> "использования"
            else -> "использований"
        }
        return "$count $word"
    }

    fun scorePercent(score: Double): String = "${(score * 100).toInt()}%"

    private val MONTHS_PREPOSITIONAL = listOf(
        "январе", "феврале", "марте", "апреле", "мае", "июне",
        "июле", "августе", "сентябре", "октябре", "ноябре", "декабре",
    )
    private val MONTHS_GENITIVE = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря",
    )
}
