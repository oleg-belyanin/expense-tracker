package com.olegbelyanin.expensetracker.domain.demo

import com.olegbelyanin.expensetracker.domain.backup.ExpenseCsv
import com.olegbelyanin.expensetracker.model.BuiltinCategories
import java.time.LocalDate
import java.time.ZoneOffset

data class DemoExpenseTemplate(val name: String, val locationName: String?, val categoryCode: String)

/**
 * Расходы для UI и замера NFR-2. Не seed: у строк есть суммы и даты.
 */
object DemoExpenseGenerator {
    const val UI_COUNT = 300
    const val NFR_COUNT = 5_000
    const val UI_PREFIX = "ui"
    const val NFR_PREFIX = "nfr"
    val ANCHOR_DATE: LocalDate = LocalDate.of(2026, 9, 4)

    fun writeCsv(
        templates: List<DemoExpenseTemplate>,
        count: Int,
        idPrefix: String,
        goldens: List<DemoExpenseTemplate> = emptyList(),
        today: LocalDate = ANCHOR_DATE,
    ): String = ExpenseCsv.write(rows(templates, count, idPrefix, goldens, today))

    fun rows(
        templates: List<DemoExpenseTemplate>,
        count: Int,
        idPrefix: String,
        goldens: List<DemoExpenseTemplate> = emptyList(),
        today: LocalDate = ANCHOR_DATE,
    ): List<ExpenseCsv.Row> {
        require(count > 0) { "count must be positive" }
        require(templates.isNotEmpty()) { "templates must not be empty" }
        val ordered = orderTemplates(templates, goldens)
        return List(count) { index ->
            val template = ordered[index % ordered.size]
            val spec = BuiltinCategories.byCode(template.categoryCode)
            val id = id(idPrefix, index)
            ExpenseCsv.Row(
                id = id,
                spentAt = spentAt(today, index),
                amountMinor = amountMinor(template.categoryCode, index),
                name = template.name,
                categoryName = spec.name,
                categoryCode = spec.code,
                locationName = template.locationName,
                comment = null,
                assignmentSource = "import",
                dedupKey = "demo:$idPrefix:${index + 1}",
            )
        }
    }

    internal fun orderTemplates(
        templates: List<DemoExpenseTemplate>,
        goldens: List<DemoExpenseTemplate>,
    ): List<DemoExpenseTemplate> {
        val remaining = templates.toMutableList()
        val head = goldens.map { golden ->
            val match = remaining.indexOfFirst { it.sameAs(golden) }
            if (match >= 0) remaining.removeAt(match) else golden
        }
        return head + remaining
    }

    internal fun amountMinor(categoryCode: String, index: Int): Long {
        val range = AMOUNTS.getValue(categoryCode)
        val span = range.last - range.first
        return range.first + (index.toLong() * 47_083L).mod(span + 1)
    }

    internal fun spentAt(today: LocalDate, index: Int) =
        today.minusDays((index % 180).toLong()).atTime(10, 0).toInstant(ZoneOffset.UTC)

    private fun id(prefix: String, index: Int): String = "demo-$prefix-${(index + 1).toString().padStart(4, '0')}"

    private fun DemoExpenseTemplate.sameAs(other: DemoExpenseTemplate): Boolean =
        name == other.name && locationName == other.locationName && categoryCode == other.categoryCode

    private val AMOUNTS: Map<String, LongRange> = mapOf(
        "GROCERIES" to 8_000L..250_000L,
        "CAFE" to 15_000L..120_000L,
        "TRANSPORT" to 5_000L..80_000L,
        "HEALTH" to 30_000L..500_000L,
        "HOUSING" to 200_000L..3_500_000L,
        "COMMUNICATION" to 25_000L..120_000L,
        "ENTERTAINMENT" to 30_000L..250_000L,
        "CLOTHING" to 80_000L..800_000L,
        "HOME" to 15_000L..600_000L,
        "OTHER" to 5_000L..150_000L,
    )
}
