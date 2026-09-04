package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.domain.backup.ExpenseCsv
import com.olegbelyanin.expensetracker.domain.demo.DemoExpenseGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.readText

class DemoExpensesArtifactTest {
    @Test
    fun committedUiDemoMatchesGeneratorAndIsNotSeedFormat() {
        val csv = DemoDataGenerator.write(
            DemoGeneratorArgs(
                train = ProjectRoot.dir().resolve("seed-data/raw/train.csv").toPath(),
                count = DemoExpenseGenerator.UI_COUNT,
                prefix = DemoExpenseGenerator.UI_PREFIX,
                outputs = emptyList(),
            ),
        )
        val committed = ProjectRoot.dir().resolve("demo-data/expenses-ui.csv").readText()
        val debugCopy = ProjectRoot.dir().resolve("app/src/debug/assets/demo/expenses.csv").readText()
        assertEquals(csv, committed)
        assertEquals(csv, debugCopy)
        val rows = ExpenseCsv.parse(committed)
        assertEquals(DemoExpenseGenerator.UI_COUNT, rows.size)
        assertTrue(committed.startsWith(ExpenseCsv.HEADER.joinToString(",")))
        assertTrue(rows.any { it.name == "Латте" && it.locationName == "Шоколадница" })
        assertTrue(rows.all { it.amountMinor > 0 })
    }
}
