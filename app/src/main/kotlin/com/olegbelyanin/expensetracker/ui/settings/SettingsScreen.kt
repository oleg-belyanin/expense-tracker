package com.olegbelyanin.expensetracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.olegbelyanin.expensetracker.LocalAppContainer
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.data.theme.ThemePreference
import com.olegbelyanin.expensetracker.ui.components.BackAction
import com.olegbelyanin.expensetracker.ui.components.BottomSheetSize
import com.olegbelyanin.expensetracker.ui.components.ExpenseBottomSheet
import com.olegbelyanin.expensetracker.ui.components.PrimaryButton
import com.olegbelyanin.expensetracker.ui.components.SelectionRow
import com.olegbelyanin.expensetracker.ui.components.SettingsRow
import com.olegbelyanin.expensetracker.ui.components.SettingsRowTone
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(themePreference: ThemePreference, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val rememberedRuleCount by container.observeRememberedRuleCount().collectAsStateWithLifecycle(0)
    var showThemeSheet by remember { mutableStateOf(false) }
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
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
            )
            SettingsRow(
                title = stringResource(R.string.settings_backup),
                subtitle = stringResource(R.string.settings_backup_subtitle),
                value = "›",
            )
            SettingsRow(
                title = stringResource(R.string.settings_restore),
                subtitle = stringResource(R.string.settings_restore_subtitle),
                value = "›",
            )
            SectionLabel(stringResource(R.string.settings_section_privacy))
            SettingsRow(
                title = stringResource(R.string.settings_rules),
                subtitle = stringResource(R.string.settings_rules_subtitle),
                value = stringResource(R.string.settings_rules_value, rememberedRuleCount),
            )
            SettingsRow(
                title = stringResource(R.string.settings_clear),
                subtitle = stringResource(R.string.settings_clear_subtitle),
                value = "›",
                tone = SettingsRowTone.Destructive,
            )
        }
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = ExpenseTheme.spacing.xs),
        style = ExpenseTheme.typography.labelSmall,
        color = ExpenseTheme.colors.textSecondary,
    )
}

private val ThemePreference.labelRes: Int
    get() =
        when (this) {
            ThemePreference.System -> R.string.theme_system
            ThemePreference.Light -> R.string.theme_light
            ThemePreference.Dark -> R.string.theme_dark
        }
