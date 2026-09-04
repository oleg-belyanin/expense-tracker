package com.olegbelyanin.expensetracker.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestorePlannerTest {
    @Test
    fun bindsBuiltinByCodeWhenLocalIdsDiffer() {
        val local = sampleBackupSnapshot(
            cafeId = 20,
            expenseId = "local-e",
            includeExpense = false,
            includeLearning = false,
        )
        val incoming = sampleBackupSnapshot(cafeId = 2)
        val plan = BackupRestorePlanner.plan(BackupIdentities.from(local), incoming)
        assertEquals(20, plan.categoryBindings.single { it.backupId == 2L }.localId)
        assertTrue(plan.categoriesToInsert.isEmpty())
        assertEquals(1, plan.expensesToInsert.size)
    }

    @Test
    fun skipsExpenseByUuidAndDedupKey() {
        val current = sampleBackupSnapshot()
        val sameId = sampleBackupSnapshot()
        val sameDedup = sampleBackupSnapshot(expenseId = "other-id")
            .let { snapshot ->
                snapshot.copy(
                    expenses = snapshot.expenses.map { it.copy(dedupKey = "user:e-1") },
                    learningExamples = emptyList(),
                )
            }
        val byId = BackupRestorePlanner.plan(BackupIdentities.from(current), sameId)
        val byDedup = BackupRestorePlanner.plan(BackupIdentities.from(current), sameDedup)
        assertEquals(1, byId.expensesSkipped)
        assertTrue(byId.expensesToInsert.isEmpty())
        assertEquals(1, byDedup.expensesSkipped)
        assertTrue(byDedup.expensesToInsert.isEmpty())
    }

    @Test
    fun skipsLearningExampleByIdAndDoesNotDoubleUserStats() {
        val current = sampleBackupSnapshot()
        val plan = BackupRestorePlanner.plan(BackupIdentities.from(current), current)
        assertEquals(1, plan.examplesSkipped)
        assertTrue(plan.examplesToInsert.isEmpty())
        assertTrue(plan.keywordStatsToInsert.isEmpty())
        assertTrue(plan.exactRulesToInsert.isEmpty())
    }

    @Test
    fun replacesSeedExactRuleWithUserRule() {
        val current = sampleBackupSnapshot(includeLearning = false).copy(
            exactRules = listOf(
                BackupExactRule("латте", 2, BackupFile.SOURCE_SEED, createdAt = 1, updatedAt = 1),
            ),
        )
        val incoming = sampleBackupSnapshot()
        val plan = BackupRestorePlanner.plan(BackupIdentities.from(current), incoming)
        assertEquals(1, plan.exactRulesToInsert.size)
        assertEquals("correction", plan.exactRulesToInsert.single().source)
    }

    @Test
    fun insertsMissingUserCategory() {
        val current = sampleBackupSnapshot(includeExpense = false, includeLearning = false)
        val incoming = current.copy(
            categories = current.categories + BackupCategory(
                id = 40,
                name = "Стоматология",
                normalizedName = "стоматолог",
                color = "#C62828",
                icon = "health",
                isBuiltin = false,
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        val plan = BackupRestorePlanner.plan(BackupIdentities.from(current), incoming)
        assertEquals(listOf(40L), plan.categoriesToInsert.map { it.id })
    }
}
