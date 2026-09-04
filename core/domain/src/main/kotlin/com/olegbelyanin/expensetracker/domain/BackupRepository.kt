package com.olegbelyanin.expensetracker.domain

import com.olegbelyanin.expensetracker.domain.backup.BackupIdentities
import com.olegbelyanin.expensetracker.domain.backup.BackupRestorePlan
import com.olegbelyanin.expensetracker.domain.backup.BackupRestoreResult
import com.olegbelyanin.expensetracker.domain.backup.BackupSnapshot

interface BackupRepository {
    suspend fun exportSnapshot(): BackupSnapshot

    suspend fun identities(): BackupIdentities

    suspend fun apply(plan: BackupRestorePlan): BackupRestoreResult
}
