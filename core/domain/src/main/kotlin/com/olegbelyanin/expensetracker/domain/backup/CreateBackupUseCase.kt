package com.olegbelyanin.expensetracker.domain.backup

import com.olegbelyanin.expensetracker.domain.BackupRepository
import java.time.Clock

class CreateBackupUseCase(private val backup: BackupRepository, private val clock: Clock) {
    suspend operator fun invoke(): String {
        val snapshot = backup.exportSnapshot().copy(exportedAtEpochMs = clock.millis())
        return BackupJson.encode(snapshot)
    }
}
