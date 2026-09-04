package com.olegbelyanin.expensetracker.ui.settings

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

object SettingsFileNames {
    const val CSV_MIME = "text/csv"
    const val BACKUP_MIME = "application/json"

    val RESTORE_MIME_TYPES: Array<String> = arrayOf("application/json", "*/*")

    fun csv(clock: Clock, zoneId: ZoneId): String = "expenses-${LocalDate.now(clock.withZone(zoneId))}.csv"

    fun backup(clock: Clock, zoneId: ZoneId): String =
        "expense-tracker-backup-${LocalDate.now(clock.withZone(zoneId))}.json"
}
