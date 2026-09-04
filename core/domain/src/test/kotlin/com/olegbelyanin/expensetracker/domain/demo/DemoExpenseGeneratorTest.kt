package com.olegbelyanin.expensetracker.domain.demo

import com.olegbelyanin.expensetracker.domain.backup.ExpenseCsv
import com.olegbelyanin.expensetracker.model.BuiltinCategories
import com.olegbelyanin.expensetracker.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class DemoExpenseGeneratorTest {
    private val templates = BuiltinCategories.all.mapIndexed { index, spec ->
        DemoExpenseTemplate("Товар $index", if (index % 2 == 0) "Магазин $index" else null, spec.code)
    }

    @Test
    fun uiSetHasAmountsDatesAndAllCategories() {
        val rows = DemoExpenseGenerator.rows(templates, DemoExpenseGenerator.UI_COUNT, DemoExpenseGenerator.UI_PREFIX)
        assertEquals(DemoExpenseGenerator.UI_COUNT, rows.size)
        assertEquals(rows.size, rows.map { it.id }.toSet().size)
        assertEquals(rows.size, rows.map { it.dedupKey }.toSet().size)
        assertTrue(rows.all { it.amountMinor > 0 })
        val latest = DemoExpenseGenerator.ANCHOR_DATE.atTime(10, 0).toInstant(ZoneOffset.UTC)
        assertTrue(rows.all { !it.spentAt.isAfter(latest) })
        assertEquals(BuiltinCategories.all.map { it.code }.toSet(), rows.map { it.categoryCode }.toSet())
        assertTrue(rows.any { !it.locationName.isNullOrBlank() })
    }

    @Test
    fun nfrSetIsFiveThousandDistinctRows() {
        val rows = DemoExpenseGenerator.rows(templates, DemoExpenseGenerator.NFR_COUNT, DemoExpenseGenerator.NFR_PREFIX)
        assertEquals(DemoExpenseGenerator.NFR_COUNT, rows.size)
        assertEquals(DemoExpenseGenerator.NFR_COUNT, rows.map { it.id }.toSet().size)
        assertTrue(rows.first().id.startsWith("demo-nfr-"))
    }

    @Test
    fun goldensLeadTheUiSet() {
        val goldens = listOf(DemoExpenseTemplate("Латте", "Шоколадница", "CAFE"))
        val rows = DemoExpenseGenerator.rows(templates, 3, "ui", goldens)
        assertEquals("Латте", rows.first().name)
        assertEquals("Шоколадница", rows.first().locationName)
    }

    @Test
    fun csvRoundTripKeepsDraftFields() {
        val rows = DemoExpenseGenerator.rows(templates, 2, "ui")
        val drafts = DemoExpenseCsv.toDrafts(ExpenseCsv.parse(ExpenseCsv.write(rows)))
        assertEquals(rows.map { it.id }, drafts.map { it.id })
        assertEquals(rows.map { Money(it.amountMinor) }, drafts.map { it.amount })
        assertEquals(rows.map { it.spentAt }, drafts.map { it.spentAt })
        assertEquals(rows.map { it.dedupKey }, drafts.map { it.dedupKey })
    }
}
