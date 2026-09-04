package com.olegbelyanin.expensetracker.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun jsonRoundTripKeepsLearningAndVersions() {
        val snapshot = sampleBackupSnapshot()
        val restored = BackupJson.decodeSnapshot(BackupJson.encode(snapshot))
        assertEquals(snapshot.copy(exportedAtEpochMs = restored.exportedAtEpochMs), restored)
        assertEquals(snapshot.expenses, restored.expenses)
        assertEquals(snapshot.exactRules, restored.exactRules)
        assertEquals(snapshot.learningExamples, restored.learningExamples)
        assertEquals(1, restored.schemaVersion)
        assertEquals(1, restored.normalizerVersion)
        assertEquals(1, restored.seedDataVersion)
    }

    @Test
    fun encodedFileDeclaresKnownFormat() {
        val json = BackupJson.encode(sampleBackupSnapshot())
        assertTrue(json.contains("\"format\": \"${BackupFile.FORMAT}\""))
        assertTrue(json.contains("\"formatVersion\": 1"))
    }

    @Test
    fun rejectsEmptyAndForeignJson() {
        assertCorrupted("")
        assertCorrupted("{")
        assertCorrupted("""{"hello":"world"}""")
        assertCorrupted(
            """
            {"format":"other","formatVersion":1,"schemaVersion":1,
            "normalizerVersion":1,"seedDataVersion":1,"exportedAt":"2026-09-03T00:00:00Z"}
            """.trimIndent(),
        )
    }

    @Test
    fun rejectsBrokenReferencesAndZeroAmount() {
        val valid = BackupJson.decode(BackupJson.encode(sampleBackupSnapshot()))
        BackupValidator.validate(valid)
        val missingCategory = valid.copy(
            expenses = valid.expenses.map { it.copy(categoryId = 404) },
        )
        assertCorrupted(missingCategory)
        val zeroAmount = valid.copy(
            expenses = valid.expenses.map { it.copy(amountMinor = 0) },
        )
        assertCorrupted(zeroAmount)
    }

    @Test
    fun rejectsUnknownSchemaVersion() {
        val file = BackupJson.decode(BackupJson.encode(sampleBackupSnapshot())).copy(schemaVersion = 99)
        try {
            BackupValidator.validate(file)
            throw AssertionError("expected BackupIncompatibleException")
        } catch (error: BackupIncompatibleException) {
            assertTrue(error.userMessage.contains("не изменены"))
        }
    }

    private fun assertCorrupted(json: String) {
        try {
            val file = BackupJson.decode(json)
            BackupValidator.validate(file)
            throw AssertionError("expected BackupCorruptedException for: $json")
        } catch (error: BackupCorruptedException) {
            assertTrue(error.userMessage.contains("не изменены"))
        }
    }

    private fun assertCorrupted(file: BackupFile) {
        try {
            BackupValidator.validate(file)
            throw AssertionError("expected BackupCorruptedException")
        } catch (error: BackupCorruptedException) {
            assertTrue(error.userMessage.contains("не изменены"))
        }
    }
}
