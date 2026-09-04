package com.olegbelyanin.expensetracker.domain.backup

import com.olegbelyanin.expensetracker.domain.BackupRepository

class RestoreBackupUseCase(private val backup: BackupRepository) {
    suspend operator fun invoke(json: String): BackupRestoreResult {
        val file = BackupJson.decode(json)
        BackupValidator.validate(file)
        val snapshot = BackupJson.snapshotOf(file)
        val identities = backup.identities()
        BackupValidator.validateAgainstDevice(file, identities)
        val plan = BackupRestorePlanner.plan(identities, snapshot)
        return backup.apply(plan)
    }
}
