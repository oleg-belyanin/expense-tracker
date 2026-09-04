package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.ui.settings.SettingsFileNames
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SettingsFileNamesTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-04T08:00:00Z"), ZoneOffset.UTC)

    @Test
    fun csvAndBackupUseLocalDate() {
        assertEquals("expenses-2026-09-04.csv", SettingsFileNames.csv(clock, ZoneOffset.UTC))
        assertEquals(
            "expense-tracker-backup-2026-09-04.json",
            SettingsFileNames.backup(clock, ZoneOffset.UTC),
        )
    }

    @Test
    fun mimeTypeFollowsExtension() {
        assertEquals("application/json", SettingsFileNames.mimeType("expense-tracker-backup-2026-09-04.json"))
        assertEquals("text/csv", SettingsFileNames.mimeType("expenses-2026-09-04.csv"))
        assertEquals("application/octet-stream", SettingsFileNames.mimeType("notes.txt"))
    }
}
