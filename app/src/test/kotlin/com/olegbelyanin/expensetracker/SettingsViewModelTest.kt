package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.data.files.SettingsDocumentStore
import com.olegbelyanin.expensetracker.domain.backup.BackupCorruptedException
import com.olegbelyanin.expensetracker.domain.backup.BackupRestoreResult
import com.olegbelyanin.expensetracker.ui.settings.SettingsBusyKind
import com.olegbelyanin.expensetracker.ui.settings.SettingsDialog
import com.olegbelyanin.expensetracker.ui.settings.SettingsOverlay
import com.olegbelyanin.expensetracker.ui.settings.SettingsToast
import com.olegbelyanin.expensetracker.domain.learning.RememberedRuleCounts
import com.olegbelyanin.expensetracker.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-09-04T08:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun exportCancelLeavesNoFile() = runTest(dispatcher) {
        val documents = InMemorySettingsDocumentStore()
        val viewModel = viewModel(documents = documents, exportCsv = { "id,name\n1,Латте" })

        viewModel.onOpenExportConfirm()
        assertEquals(SettingsDialog.ExportConfirm, viewModel.dialog.value)
        viewModel.onDismissDialog()

        assertEquals(SettingsDialog.None, viewModel.dialog.value)
        assertTrue(documents.files.isEmpty())
        assertEquals(null, viewModel.toast.value)
    }

    @Test
    fun exportWritesCsvAndShowsShareToast() = runTest(dispatcher) {
        val documents = InMemorySettingsDocumentStore()
        val viewModel =
            viewModel(
                documents = documents,
                exportCsv = { "id,name\n1,Латте" },
            )

        viewModel.onOpenExportConfirm()
        viewModel.onConfirmExport()
        runCurrent()

        assertEquals("id,name\n1,Латте", documents.files["content://shared/expenses-2026-09-04.csv"])
        assertEquals(
            SettingsToast.ExportDone("content://shared/expenses-2026-09-04.csv"),
            viewModel.toast.value,
        )
        assertEquals(SettingsOverlay.None, viewModel.overlay.value)
        viewModel.onDismissToast()
    }

    @Test
    fun backupCancelLeavesNoFile() = runTest(dispatcher) {
        val documents = InMemorySettingsDocumentStore()
        val viewModel = viewModel(documents = documents, createBackup = { "{}" })

        viewModel.onOpenBackupConfirm()
        assertEquals(SettingsDialog.BackupConfirm, viewModel.dialog.value)
        viewModel.onDismissDialog()

        assertEquals(SettingsDialog.None, viewModel.dialog.value)
        assertTrue(documents.files.isEmpty())
        assertEquals(null, viewModel.toast.value)
    }

    @Test
    fun backupWriteFailureShowsHumanToastAndKeepsOverlayClear() = runTest(dispatcher) {
        val documents = InMemorySettingsDocumentStore(writeShouldFail = true)
        val viewModel = viewModel(documents = documents, createBackup = { "{}" })

        viewModel.onOpenBackupConfirm()
        viewModel.onConfirmBackup()
        runCurrent()

        assertTrue(documents.files.isEmpty())
        assertEquals(SettingsToast.SaveFailed, viewModel.toast.value)
        assertEquals(SettingsOverlay.None, viewModel.overlay.value)
        viewModel.onDismissToast()
    }

    @Test
    fun corruptedRestoreShowsFailedOverlayAndDoesNotApply() = runTest(dispatcher) {
        var restoreCalls = 0
        val documents = InMemorySettingsDocumentStore(files = mutableMapOf("content://bad.json" to "{}"))
        val viewModel =
            viewModel(
                documents = documents,
                restoreBackup = {
                    restoreCalls += 1
                    throw BackupCorruptedException()
                },
            )

        viewModel.onRestoreDocument("content://bad.json")
        runCurrent()

        assertEquals(1, restoreCalls)
        val overlay = viewModel.overlay.value as SettingsOverlay.RestoreFailed
        assertTrue(overlay.message.contains("не изменены"))
        assertEquals(null, viewModel.toast.value)
    }

    @Test
    fun restoreBusyUsesCheckingOverlay() = runTest(dispatcher) {
        val documents = InMemorySettingsDocumentStore(files = mutableMapOf("content://ok.json" to "{}"))
        val viewModel =
            viewModel(
                documents = documents,
                restoreBackup = {
                    BackupRestoreResult(1, 0, 0, 0, 0, 0, 0)
                },
            )

        viewModel.onRestoreDocument("content://ok.json")
        assertEquals(SettingsOverlay.Busy(SettingsBusyKind.Restore), viewModel.overlay.value)
        runCurrent()
        assertEquals(SettingsToast.RestoreDone(1, 0), viewModel.toast.value)
        viewModel.onDismissToast()
    }

    @Test
    fun clearHistoryRequiresConfirmThenKeepsGoing() = runTest(dispatcher) {
        var cleared = 0
        val viewModel =
            viewModel(
                clearHistory = {
                    cleared += 1
                    4
                },
            )

        viewModel.onOpenClearConfirm()
        assertEquals(SettingsDialog.ClearConfirm, viewModel.dialog.value)
        viewModel.onConfirmClearHistory()
        runCurrent()

        assertEquals(1, cleared)
        assertEquals(SettingsDialog.None, viewModel.dialog.value)
        assertEquals(SettingsToast.HistoryCleared, viewModel.toast.value)
        viewModel.onDismissToast()
    }

    @Test
    fun suggestedNamesFollowClock() {
        val viewModel = viewModel()
        assertEquals("expenses-2026-09-04.csv", viewModel.csvFileName())
        assertEquals("expense-tracker-backup-2026-09-04.json", viewModel.backupFileName())
    }

    private fun viewModel(
        documents: SettingsDocumentStore = InMemorySettingsDocumentStore(),
        exportCsv: suspend () -> String = { "" },
        createBackup: suspend () -> String = { "" },
        restoreBackup: suspend (String) -> BackupRestoreResult = {
            BackupRestoreResult(0, 0, 0, 0, 0, 0, 0)
        },
        clearHistory: suspend () -> Int = { 0 },
    ) = SettingsViewModel(
        exportCsv = exportCsv,
        createBackup = createBackup,
        restoreBackup = restoreBackup,
        clearHistory = clearHistory,
        rememberedRuleCounts = MutableStateFlow(RememberedRuleCounts(12, 8, 3)),
        documents = documents,
        clock = clock,
        zoneId = ZoneOffset.UTC,
    )
}

private class InMemorySettingsDocumentStore(
    val files: MutableMap<String, String> = mutableMapOf(),
    private val writeShouldFail: Boolean = false,
) : SettingsDocumentStore {
    override suspend fun writeText(uri: String, text: String) {
        if (writeShouldFail) error("disk")
        files[uri] = text
    }

    override suspend fun writeSharedFile(fileName: String, text: String): String {
        if (writeShouldFail) error("disk")
        val uri = "content://shared/$fileName"
        files[uri] = text
        return uri
    }

    override suspend fun readText(uri: String): String = files.getValue(uri)
}
