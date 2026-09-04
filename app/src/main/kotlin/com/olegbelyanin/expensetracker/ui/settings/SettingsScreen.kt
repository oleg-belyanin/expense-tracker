package com.olegbelyanin.expensetracker.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olegbelyanin.expensetracker.LocalAppContainer
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.data.theme.ThemePreference
import com.olegbelyanin.expensetracker.ui.components.BackAction
import com.olegbelyanin.expensetracker.ui.components.BottomSheetSize
import com.olegbelyanin.expensetracker.ui.components.ButtonTone
import com.olegbelyanin.expensetracker.ui.components.DestructiveDialog
import com.olegbelyanin.expensetracker.ui.components.ExpenseBottomSheet
import com.olegbelyanin.expensetracker.ui.components.ExpenseToast
import com.olegbelyanin.expensetracker.ui.components.PrimaryButton
import com.olegbelyanin.expensetracker.ui.components.SelectionRow
import com.olegbelyanin.expensetracker.ui.components.SettingsRow
import com.olegbelyanin.expensetracker.ui.components.SettingsRowTone
import com.olegbelyanin.expensetracker.ui.components.StatePanel
import com.olegbelyanin.expensetracker.ui.components.StatePanelType
import com.olegbelyanin.expensetracker.ui.components.ToastTone
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themePreference: ThemePreference,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rememberedRuleCounts by viewModel.rememberedRuleCounts.collectAsStateWithLifecycle()
    val overlay by viewModel.overlay.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    var showThemeSheet by remember { mutableStateOf(false) }
    val openBackup =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.onRestoreDocument(it.toString()) }
        }

    fun share(uri: String, mimeType: String) {
        try {
            shareExportedDocument(context, uri, mimeType)
            viewModel.onDismissToast()
        } catch (_: ActivityNotFoundException) {
            viewModel.onShareUnavailable()
        }
    }

    BackHandler(enabled = overlay is SettingsOverlay.RestoreFailed) {
        viewModel.onDismissOverlay()
    }
    Box(
        modifier =
        modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.xs, end = spacing.md, top = spacing.md, bottom = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackAction(onClick = onBack)
                Text(
                    text = stringResource(R.string.settings),
                    style = typography.titleSection,
                    color = colors.textPrimary,
                )
            }
            Column(
                modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                SectionLabel(stringResource(R.string.settings_section_appearance))
                SettingsRow(
                    title = stringResource(R.string.settings_theme),
                    subtitle = stringResource(R.string.settings_theme_subtitle),
                    value = stringResource(themePreference.labelRes),
                    onClick = { showThemeSheet = true },
                )
                SectionLabel(stringResource(R.string.settings_section_data))
                SettingsRow(
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_subtitle),
                    value = "›",
                    onClick = viewModel::onOpenExportConfirm,
                )
                SettingsRow(
                    title = stringResource(R.string.settings_backup),
                    subtitle = stringResource(R.string.settings_backup_subtitle),
                    value = "›",
                    onClick = viewModel::onOpenBackupConfirm,
                )
                SettingsRow(
                    title = stringResource(R.string.settings_restore),
                    subtitle = stringResource(R.string.settings_restore_subtitle),
                    value = "›",
                    onClick = { openBackup.launch(SettingsFileNames.RESTORE_MIME_TYPES) },
                )
                SectionLabel(stringResource(R.string.settings_section_privacy))
                SettingsRow(
                    title = stringResource(R.string.settings_rules_exact),
                    subtitle = stringResource(R.string.settings_rules_exact_subtitle),
                    value = stringResource(R.string.settings_rules_value, rememberedRuleCounts.exactRules),
                )
                SettingsRow(
                    title = stringResource(R.string.settings_rules_keywords),
                    subtitle = stringResource(R.string.settings_rules_keywords_subtitle),
                    value = stringResource(R.string.settings_rules_value, rememberedRuleCounts.keywordRules),
                )
                SettingsRow(
                    title = stringResource(R.string.settings_rules_locations),
                    subtitle = stringResource(R.string.settings_rules_locations_subtitle),
                    value = stringResource(R.string.settings_rules_value, rememberedRuleCounts.locationRules),
                )
                SectionLabel(stringResource(R.string.settings_section_seed))
                SettingsRow(
                    title = stringResource(R.string.settings_rules_seed_exact),
                    subtitle = stringResource(R.string.settings_rules_seed_exact_subtitle),
                    value = stringResource(R.string.settings_rules_value, rememberedRuleCounts.seedExactRules),
                )
                SettingsRow(
                    title = stringResource(R.string.settings_rules_seed_keywords),
                    subtitle = stringResource(R.string.settings_rules_seed_keywords_subtitle),
                    value = stringResource(R.string.settings_rules_value, rememberedRuleCounts.seedKeywordRules),
                )
                SettingsRow(
                    title = stringResource(R.string.settings_rules_seed_locations),
                    subtitle = stringResource(R.string.settings_rules_seed_locations_subtitle),
                    value = stringResource(R.string.settings_rules_value, rememberedRuleCounts.seedLocationRules),
                )
                SettingsRow(
                    title = stringResource(R.string.settings_clear),
                    subtitle = stringResource(R.string.settings_clear_subtitle),
                    value = "›",
                    tone = SettingsRowTone.Destructive,
                    onClick = viewModel::onOpenClearConfirm,
                )
            }
        }
        toast?.let { current ->
            SettingsToastBar(
                toast = current,
                onShare = ::share,
                modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = spacing.md, end = spacing.md, bottom = spacing.md),
            )
        }
        when (val current = overlay) {
            SettingsOverlay.None -> Unit

            is SettingsOverlay.Busy ->
                SettingsStatusOverlay(
                    type = StatePanelType.Loading,
                    title = stringResource(current.kind.titleRes),
                    description = stringResource(current.kind.descriptionRes),
                    dismissible = false,
                    onDismiss = {},
                )

            is SettingsOverlay.RestoreFailed ->
                SettingsStatusOverlay(
                    type = StatePanelType.NoResults,
                    title = stringResource(R.string.settings_restore_failed_title),
                    description = current.message,
                    actionLabel = stringResource(R.string.settings_restore_pick_another),
                    dismissible = true,
                    onDismiss = viewModel::onDismissOverlay,
                    onAction = {
                        viewModel.onDismissOverlay()
                        openBackup.launch(SettingsFileNames.RESTORE_MIME_TYPES)
                    },
                )
        }
    }
    when (dialog) {
        SettingsDialog.None -> Unit

        SettingsDialog.ExportConfirm ->
            DestructiveDialog(
                title = stringResource(R.string.settings_export_confirm_title),
                description = stringResource(R.string.settings_export_confirm_description),
                confirmLabel = stringResource(R.string.settings_export_confirm),
                cancelLabel = stringResource(R.string.cancel),
                onConfirm = viewModel::onConfirmExport,
                onDismiss = viewModel::onDismissDialog,
                confirmTone = ButtonTone.Primary,
            )

        SettingsDialog.BackupConfirm ->
            DestructiveDialog(
                title = stringResource(R.string.settings_backup_confirm_title),
                description = stringResource(R.string.settings_backup_confirm_description),
                confirmLabel = stringResource(R.string.settings_backup_confirm),
                cancelLabel = stringResource(R.string.cancel),
                onConfirm = viewModel::onConfirmBackup,
                onDismiss = viewModel::onDismissDialog,
                confirmTone = ButtonTone.Primary,
            )

        SettingsDialog.ClearConfirm ->
            DestructiveDialog(
                title = stringResource(R.string.settings_clear_title),
                description = stringResource(R.string.settings_clear_description),
                confirmLabel = stringResource(R.string.settings_clear_confirm),
                cancelLabel = stringResource(R.string.cancel),
                onConfirm = viewModel::onConfirmClearHistory,
                onDismiss = viewModel::onDismissDialog,
            )
    }
    if (showThemeSheet) {
        ExpenseBottomSheet(
            onDismiss = { showThemeSheet = false },
            size = BottomSheetSize.Tall,
        ) {
            Text(
                text = stringResource(R.string.theme_sheet_title),
                style = typography.titleSection,
                color = colors.textPrimary,
            )
            SelectionRow(
                title = stringResource(R.string.theme_system),
                subtitle = stringResource(R.string.theme_system_detail),
                selected = themePreference == ThemePreference.System,
                onClick = { scope.launch { container.themeRepository.setTheme(ThemePreference.System) } },
            )
            SelectionRow(
                title = stringResource(R.string.theme_light),
                subtitle = stringResource(R.string.theme_light_detail),
                selected = themePreference == ThemePreference.Light,
                onClick = { scope.launch { container.themeRepository.setTheme(ThemePreference.Light) } },
            )
            SelectionRow(
                title = stringResource(R.string.theme_dark),
                subtitle = stringResource(R.string.theme_dark_detail),
                selected = themePreference == ThemePreference.Dark,
                onClick = { scope.launch { container.themeRepository.setTheme(ThemePreference.Dark) } },
            )
            Text(
                text = stringResource(R.string.theme_sheet_note),
                style = typography.labelSmall,
                color = colors.textSecondary,
            )
            PrimaryButton(
                label = stringResource(R.string.done),
                onClick = { showThemeSheet = false },
            )
        }
    }
}

@Composable
private fun SettingsToastBar(
    toast: SettingsToast,
    onShare: (uri: String, mimeType: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (message, tone, actionLabel, onAction) =
        when (toast) {
            is SettingsToast.ExportDone ->
                ToastContent(
                    message = stringResource(R.string.settings_export_done),
                    tone = ToastTone.Success,
                    actionLabel = stringResource(R.string.share),
                    onAction = { onShare(toast.uri, SettingsFileNames.CSV_MIME) },
                )

            is SettingsToast.BackupDone ->
                ToastContent(
                    message = stringResource(R.string.settings_backup_done),
                    tone = ToastTone.Success,
                    actionLabel = stringResource(R.string.share),
                    onAction = { onShare(toast.uri, SettingsFileNames.BACKUP_MIME) },
                )

            is SettingsToast.RestoreDone ->
                ToastContent(
                    message =
                    if (toast.inserted > 0) {
                        stringResource(R.string.settings_restore_done)
                    } else {
                        stringResource(R.string.settings_restore_duplicates)
                    },
                    tone = ToastTone.Success,
                )

            SettingsToast.HistoryCleared ->
                ToastContent(
                    message = stringResource(R.string.settings_clear_done),
                    tone = ToastTone.Success,
                )

            SettingsToast.SaveFailed ->
                ToastContent(
                    message = stringResource(R.string.settings_save_failed),
                    tone = ToastTone.Error,
                )

            SettingsToast.ShareUnavailable ->
                ToastContent(
                    message = stringResource(R.string.settings_share_unavailable),
                    tone = ToastTone.Error,
                )
        }
    ExpenseToast(
        message = message,
        tone = tone,
        actionLabel = actionLabel,
        onAction = onAction,
        modifier = modifier,
    )
}

@Composable
private fun SettingsStatusOverlay(
    type: StatePanelType,
    title: String,
    description: String,
    dismissible: Boolean,
    onDismiss: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = ExpenseTheme.colors
    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (dismissible) onDismiss() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
            Modifier
                .width(328.dp)
                .background(colors.background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            StatePanel(
                type = type,
                title = title,
                description = description,
                actionLabel = actionLabel,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = ExpenseTheme.spacing.xs),
        style = ExpenseTheme.typography.labelSmall,
        color = ExpenseTheme.colors.textSecondary,
    )
}

private data class ToastContent(
    val message: String,
    val tone: ToastTone,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

private val SettingsBusyKind.titleRes: Int
    get() =
        when (this) {
            SettingsBusyKind.Export -> R.string.settings_export_loading_title
            SettingsBusyKind.Backup -> R.string.settings_backup_loading_title
            SettingsBusyKind.Restore -> R.string.settings_restore_loading_title
            SettingsBusyKind.Clear -> R.string.settings_clear_loading_title
        }

private val SettingsBusyKind.descriptionRes: Int
    get() =
        when (this) {
            SettingsBusyKind.Export -> R.string.settings_export_loading_description
            SettingsBusyKind.Backup -> R.string.settings_backup_loading_description
            SettingsBusyKind.Restore -> R.string.settings_restore_loading_description
            SettingsBusyKind.Clear -> R.string.settings_clear_loading_description
        }

private val ThemePreference.labelRes: Int
    get() =
        when (this) {
            ThemePreference.System -> R.string.theme_system
            ThemePreference.Light -> R.string.theme_light
            ThemePreference.Dark -> R.string.theme_dark
        }
