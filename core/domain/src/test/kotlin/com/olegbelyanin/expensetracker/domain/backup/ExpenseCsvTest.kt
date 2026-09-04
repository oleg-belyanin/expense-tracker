package com.olegbelyanin.expensetracker.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExpenseCsvTest {
    @Test
    fun writesHeaderAndCategoryName() {
        val csv = ExpenseCsv.write(listOf(sampleRow()))
        val lines = csv.trim().split('\n')
        assertEquals(ExpenseCsv.HEADER.joinToString(","), lines.first())
        assertTrue(csv.contains("Кафе"))
        assertTrue(csv.contains("CAFE"))
        assertTrue(csv.contains("150.50"))
        assertTrue(csv.contains("15050"))
        assertTrue(csv.contains(ExpenseCsv.CURRENCY))
    }

    @Test
    fun escapesCommasQuotesAndNewlines() {
        val csv = ExpenseCsv.write(
            listOf(
                sampleRow(
                    name = "Кофе, \"латте\"",
                    comment = "строка\nс переносом",
                ),
            ),
        )
        val parsed = ExpenseCsv.parse(csv).single()
        assertEquals("Кофе, \"латте\"", parsed.name)
        assertEquals("строка\nс переносом", parsed.comment)
        assertEquals("Кафе", parsed.categoryName)
    }

    @Test
    fun roundTripsAllSignificantFields() {
        val original = sampleRow()
        val parsed = ExpenseCsv.parse(ExpenseCsv.write(listOf(original))).single()
        assertEquals(original, parsed)
    }

    @Test
    fun formatsRublesWithKopecks() {
        assertEquals("0.01", ExpenseCsv.formatAmount(1))
        assertEquals("12.00", ExpenseCsv.formatAmount(1_200))
        assertEquals("150.50", ExpenseCsv.formatAmount(15_050))
    }
}

private fun sampleRow(name: String = "Латте", comment: String? = "утром") = ExpenseCsv.Row(
    id = "e-1",
    spentAt = Instant.parse("2026-09-03T00:00:00Z"),
    amountMinor = 15_050,
    name = name,
    categoryName = "Кафе",
    categoryCode = "CAFE",
    locationName = "Шоколадница",
    comment = comment,
    assignmentSource = "probabilistic",
    dedupKey = "user:e-1",
)
