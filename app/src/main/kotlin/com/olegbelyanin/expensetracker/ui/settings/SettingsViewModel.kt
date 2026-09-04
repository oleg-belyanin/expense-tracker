package com.olegbelyanin.expensetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.olegbelyanin.expensetracker.data.files.SettingsDocumentStore
import com.olegbelyanin.expensetracker.domain.backup.BackupException
import com.olegbelyanin.expensetracker.domain.backup.BackupRestoreResult
import com.olegbelyanin.expensetracker.domain.backup.CreateBackupUseCase
import com.olegbelyanin.expensetracker.domain.backup.ExportExpensesCsvUseCase
import com.olegbelyanin.expensetracker.domain.backup.RestoreBackupUseCase
import com.olegbelyanin.expensetracker.domain.expense.ClearExpenseHistoryUseCase
import com.olegbelyanin.expensetracker.domain.learning.ObserveRememberedRuleCountUseCase
import com.olegbelyanin.expensetracker.domain.learning.RememberedRuleCounts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId

enum class SettingsBusyKind {
    Export,
    Backup,
    Restore,
    Clear,
}

sealed interface SettingsOverlay {
    data object None : SettingsOverlay

    data class Busy(val kind: SettingsBusyKind) : SettingsOverlay

    data class RestoreFailed(val message: String) : SettingsOverlay
}

sealed interface SettingsDialog {
    data object None : SettingsDialog

    data object ClearConfirm : SettingsDialog
}

sealed interface SettingsToast {
    data class ExportDone(val uri: String) : SettingsToast

    data class BackupDone(val uri: String) : SettingsToast

    data class RestoreDone(val inserted: Int, val skipped: Int) : SettingsToast

    data object HistoryCleared : SettingsToast

    data object SaveFailed : SettingsToast

    data object ShareUnavailable : SettingsToast
}

class SettingsViewModel(
    private val exportCsv: suspend () -> String,
    private val createBackup: suspend () -> String,
    private val restoreBackup: suspend (String) -> BackupRestoreResult,
    private val clearHistory: suspend () -> Int,
    rememberedRuleCounts: Flow<RememberedRuleCounts>,
    private val documents: SettingsDocumentStore,
    private val clock: Clock,
    private val zoneId: ZoneId,
) : ViewModel() {
    private val overlayState = MutableStateFlow<SettingsOverlay>(SettingsOverlay.None)
    private val dialogState = MutableStateFlow<SettingsDialog>(SettingsDialog.None)
    private val toastState = MutableStateFlow<SettingsToast?>(null)
    private var toastJob: Job? = null

    val rememberedRuleCounts: StateFlow<RememberedRuleCounts> =
        rememberedRuleCounts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RememberedRuleCounts(),
        )
    val overlay: StateFlow<SettingsOverlay> = overlayState.asStateFlow()
    val dialog: StateFlow<SettingsDialog> = dialogState.asStateFlow()
    val toast: StateFlow<SettingsToast?> = toastState.asStateFlow()

    fun csvFileName(): String = SettingsFileNames.csv(clock, zoneId)

    fun backupFileName(): String = SettingsFileNames.backup(clock, zoneId)

    fun onExportDocument(uri: String) {
        runFileJob(SettingsBusyKind.Export) {
            documents.writeText(uri, exportCsv())
            showToast(SettingsToast.ExportDone(uri))
        }
    }

    fun onBackupDocument(uri: String) {
        runFileJob(SettingsBusyKind.Backup) {
            documents.writeText(uri, createBackup())
            showToast(SettingsToast.BackupDone(uri))
        }
    }

    fun onRestoreDocument(uri: String) {
        runFileJob(SettingsBusyKind.Restore) {
            val result = restoreBackup(documents.readText(uri))
            showToast(SettingsToast.RestoreDone(result.expensesInserted, result.expensesSkipped))
        }
    }

    fun onOpenClearConfirm() {
        if (overlayState.value is SettingsOverlay.Busy) return
        dialogState.value = SettingsDialog.ClearConfirm
    }

    fun onDismissDialog() {
        dialogState.value = SettingsDialog.None
    }

    fun onConfirmClearHistory() {
        if (overlayState.value is SettingsOverlay.Busy) return
        dialogState.value = SettingsDialog.None
        runFileJob(SettingsBusyKind.Clear) {
            clearHistory()
            showToast(SettingsToast.HistoryCleared)
        }
    }

    fun onDismissOverlay() {
        if (overlayState.value is SettingsOverlay.Busy) return
        overlayState.value = SettingsOverlay.None
    }

    fun onDismissToast() {
        toastJob?.cancel()
        toastState.value = null
    }

    fun onShareUnavailable() {
        showToast(SettingsToast.ShareUnavailable)
    }

    private fun runFileJob(kind: SettingsBusyKind, block: suspend () -> Unit) {
        if (overlayState.value is SettingsOverlay.Busy) return
        overlayState.value = SettingsOverlay.Busy(kind)
        viewModelScope.launch {
            try {
                block()
                overlayState.value = SettingsOverlay.None
            } catch (error: CancellationException) {
                overlayState.value = SettingsOverlay.None
                throw error
            } catch (error: BackupException) {
                overlayState.value = SettingsOverlay.RestoreFailed(error.userMessage)
            } catch (_: Exception) {
                if (kind == SettingsBusyKind.Restore) {
                    overlayState.value = SettingsOverlay.RestoreFailed(RESTORE_GENERIC)
                } else {
                    overlayState.value = SettingsOverlay.None
                    showToast(SettingsToast.SaveFailed)
                }
            }
        }
    }

    private fun showToast(toast: SettingsToast) {
        toastState.value = toast
        toastJob?.cancel()
        toastJob =
            viewModelScope.launch {
                delay(TOAST_MS)
                if (toastState.value == toast) {
                    toastState.value = null
                }
            }
    }

    companion object {
        const val RESTORE_GENERIC =
            "Копия повреждена или имеет другой формат. Ваши данные не изменены."
        private const val TOAST_MS = 4_000L

        fun factory(
            exportCsv: ExportExpensesCsvUseCase,
            createBackup: CreateBackupUseCase,
            restoreBackup: RestoreBackupUseCase,
            clearHistory: ClearExpenseHistoryUseCase,
            observeRememberedRuleCount: ObserveRememberedRuleCountUseCase,
            documents: SettingsDocumentStore,
            clock: Clock,
            zoneId: ZoneId,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(
                    exportCsv = exportCsv::invoke,
                    createBackup = createBackup::invoke,
                    restoreBackup = restoreBackup::invoke,
                    clearHistory = clearHistory::invoke,
                    rememberedRuleCounts = observeRememberedRuleCount(),
                    documents = documents,
                    clock = clock,
                    zoneId = zoneId,
                ) as T
            }
        }
    }
}
