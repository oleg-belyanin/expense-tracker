package com.olegbelyanin.expensetracker.ui.format

import com.olegbelyanin.expensetracker.domain.expense.DayRelative
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.ExpenseNameSuggestion
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.model.Period
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
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

    fun periodSubtitle(preset: ExpensePeriodPreset, today: LocalDate, custom: Period? = null): String = when (preset) {
        ExpensePeriodPreset.ALL -> "За всё время"

        ExpensePeriodPreset.CURRENT_MONTH -> "В ${MONTHS_PREPOSITIONAL[today.monthValue - 1]}"

        ExpensePeriodPreset.PREVIOUS_MONTH -> {
            val month = today.minusMonths(1)
            "В ${MONTHS_PREPOSITIONAL[month.monthValue - 1]}"
        }

        ExpensePeriodPreset.YEAR -> "В ${today.year} году"

        ExpensePeriodPreset.CUSTOM -> custom?.let { dateRange(it) } ?: "Произвольный диапазон"
    }

    fun monthYear(month: YearMonth, capitalize: Boolean = false): String {
        val name = MONTHS_NOMINATIVE[month.monthValue - 1]
        val label = if (capitalize) name.replaceFirstChar { it.titlecase(Locale("ru")) } else name
        return "$label ${month.year}"
    }

    fun periodCaption(preset: ExpensePeriodPreset): String = when (preset) {
        ExpensePeriodPreset.ALL -> "Всё время"
        ExpensePeriodPreset.CURRENT_MONTH -> "Текущий месяц"
        ExpensePeriodPreset.PREVIOUS_MONTH -> "Предыдущий месяц"
        ExpensePeriodPreset.YEAR -> "Текущий год"
        ExpensePeriodPreset.CUSTOM -> "Произвольный диапазон"
    }

    fun periodTitle(preset: ExpensePeriodPreset, today: LocalDate, custom: Period? = null): String = when (preset) {
        ExpensePeriodPreset.ALL -> "Всё время"

        ExpensePeriodPreset.CURRENT_MONTH -> monthYear(YearMonth.from(today), capitalize = true)

        ExpensePeriodPreset.PREVIOUS_MONTH ->
            monthYear(YearMonth.from(today).minusMonths(1), capitalize = true)

        ExpensePeriodPreset.YEAR -> yearToDate(today)

        ExpensePeriodPreset.CUSTOM -> custom?.let { dateRange(it) } ?: yearToDate(today)
    }

    fun periodDetail(preset: ExpensePeriodPreset, today: LocalDate, custom: Period? = null): String = when (preset) {
        ExpensePeriodPreset.ALL -> "Все сохранённые расходы"

        ExpensePeriodPreset.CURRENT_MONTH -> monthYear(YearMonth.from(today))

        ExpensePeriodPreset.PREVIOUS_MONTH -> monthYear(YearMonth.from(today).minusMonths(1))

        ExpensePeriodPreset.YEAR -> yearToDate(today)

        ExpensePeriodPreset.CUSTOM -> custom?.let { dateRange(it) } ?: dateRange(
            Period(today.minusMonths(1).withDayOfMonth(1), today),
        )
    }

    fun analyticsHeader(
        totalMinor: Long,
        preset: ExpensePeriodPreset,
        today: LocalDate,
        custom: Period? = null,
    ): String = "${money(totalMinor)} · ${headerPeriod(preset, today, custom)}"

    fun shareLine(amountMinor: Long, percent: Int): String = "${money(amountMinor)} · $percent%"

    fun dateRange(period: Period): String {
        val start = period.startInclusive
        val end = period.endInclusive
        val startText = "${start.dayOfMonth} ${MONTHS_GENITIVE[start.monthValue - 1]}"
        val endText = "${end.dayOfMonth} ${MONTHS_GENITIVE[end.monthValue - 1]}"
        return if (start.year == end.year) {
            "$startText — $endText"
        } else {
            "$startText ${start.year} — $endText ${end.year}"
        }
    }

    fun periodChip(preset: ExpensePeriodPreset, today: LocalDate, custom: Period? = null): String = when (preset) {
        ExpensePeriodPreset.ALL -> "Все"

        ExpensePeriodPreset.CURRENT_MONTH -> MONTHS_SHORT[today.monthValue - 1]

        ExpensePeriodPreset.PREVIOUS_MONTH ->
            MONTHS_SHORT[today.minusMonths(1).monthValue - 1]

        ExpensePeriodPreset.YEAR -> today.year.toString()

        ExpensePeriodPreset.CUSTOM -> custom?.let { shortRange(it) } ?: "Период"
    }

    fun emptyPeriodLabel(preset: ExpensePeriodPreset, today: LocalDate, custom: Period? = null): String =
        when (preset) {
            ExpensePeriodPreset.ALL -> "всё время"

            ExpensePeriodPreset.CURRENT_MONTH -> MONTHS_NOMINATIVE[today.monthValue - 1]

            ExpensePeriodPreset.PREVIOUS_MONTH ->
                MONTHS_NOMINATIVE[today.minusMonths(1).monthValue - 1]

            ExpensePeriodPreset.YEAR -> "${today.year} год"

            ExpensePeriodPreset.CUSTOM -> custom?.let { dateRange(it) } ?: "выбранный период"
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

    fun nameDetail(suggestion: ExpenseNameSuggestion, today: LocalDate, zoneId: ZoneId): String {
        val lastUsed = suggestion.lastUsedAt?.atZone(zoneId)?.toLocalDate()
        return when (lastUsed) {
            today -> "Последнее использование — сегодня"

            today.minusDays(1) -> "Последнее использование — вчера"

            else -> if (suggestion.fromDictionary && suggestion.usageCount == 0) {
                "По словарю"
            } else {
                usages(suggestion.usageCount)
            }
        }
    }

    fun locationDetail(location: Location, today: LocalDate, zoneId: ZoneId): String {
        val lastUsed = location.lastUsedAt?.atZone(zoneId)?.toLocalDate()
        return when (lastUsed) {
            today -> "Последнее использование — сегодня"
            today.minusDays(1) -> "Последнее использование — вчера"
            else -> usages(location.usageCount)
        }
    }

    fun usages(count: Int): String = russianCount(count, "использование", "использования", "использований")

    fun categoryCount(count: Int): String = russianCount(count, "категория", "категории", "категорий")

    fun expenseCount(count: Int): String = russianCount(count, "расход", "расхода", "расходов")

    fun archivedDay(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "сегодня"
        today.minusDays(1) -> "вчера"
        else -> calendarDay(date)
    }

    private fun russianCount(count: Int, one: String, few: String, many: String): String {
        val n10 = count % 10
        val n100 = count % 100
        val word = when {
            n10 == 1 && n100 != 11 -> one
            n10 in 2..4 && n100 !in 12..14 -> few
            else -> many
        }
        return "$count $word"
    }

    fun scorePercent(score: Double): String = "${(score * 100).toInt()}%"

    private fun headerPeriod(preset: ExpensePeriodPreset, today: LocalDate, custom: Period?): String = when (preset) {
        ExpensePeriodPreset.ALL -> "всё время"
        ExpensePeriodPreset.CURRENT_MONTH -> monthYear(YearMonth.from(today))
        ExpensePeriodPreset.PREVIOUS_MONTH -> monthYear(YearMonth.from(today).minusMonths(1))
        ExpensePeriodPreset.YEAR -> today.year.toString()
        ExpensePeriodPreset.CUSTOM -> custom?.let { dateRange(it) } ?: monthYear(YearMonth.from(today))
    }

    private fun yearToDate(today: LocalDate): String {
        val start = MONTHS_NOMINATIVE[0].replaceFirstChar { it.titlecase(Locale("ru")) }
        val end = MONTHS_NOMINATIVE[today.monthValue - 1]
        return "$start — $end ${today.year}"
    }

    private fun shortRange(period: Period): String {
        val start = period.startInclusive
        val end = period.endInclusive
        return if (start.month == end.month && start.year == end.year) {
            MONTHS_SHORT[start.monthValue - 1]
        } else {
            "${MONTHS_SHORT[start.monthValue - 1]}–${MONTHS_SHORT[end.monthValue - 1]}"
        }
    }

    private val MONTHS_PREPOSITIONAL = listOf(
        "январе", "феврале", "марте", "апреле", "мае", "июне",
        "июле", "августе", "сентябре", "октябре", "ноябре", "декабре",
    )
    private val MONTHS_GENITIVE = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря",
    )
    private val MONTHS_NOMINATIVE = listOf(
        "январь", "февраль", "март", "апрель", "май", "июнь",
        "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
    )
    private val MONTHS_SHORT = listOf(
        "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
        "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек",
    )
}
