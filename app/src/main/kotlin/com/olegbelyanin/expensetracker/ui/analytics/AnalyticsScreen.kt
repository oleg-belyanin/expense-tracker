package com.olegbelyanin.expensetracker.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.domain.expense.AnalyticsCategoryRow
import com.olegbelyanin.expensetracker.domain.expense.AnalyticsSlice
import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.ui.components.CategoryAvatar
import com.olegbelyanin.expensetracker.ui.components.ExpenseDatePicker
import com.olegbelyanin.expensetracker.ui.components.FilterChip
import com.olegbelyanin.expensetracker.ui.components.PeriodSheet
import com.olegbelyanin.expensetracker.ui.components.PrimaryButton
import com.olegbelyanin.expensetracker.ui.components.toVisual
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import com.olegbelyanin.expensetracker.ui.navigation.AppTab
import com.olegbelyanin.expensetracker.ui.navigation.TabScaffold
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import com.olegbelyanin.expensetracker.ui.theme.MinTapTarget
import java.time.LocalDate

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onOpenSettings: () -> Unit,
    onTabSelected: (AppTab) -> Unit,
    onOpenFilteredExpenses: (ExpenseListFilter) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = viewModel.today,
) {
    val slice by viewModel.slice.collectAsStateWithLifecycle()
    val selection by viewModel.selectionState.collectAsStateWithLifecycle()
    val chart by viewModel.chartState.collectAsStateWithLifecycle()
    val dialog by viewModel.dialogState.collectAsStateWithLifecycle()
    val draftPreset by viewModel.draftPresetState.collectAsStateWithLifecycle()
    val draftCustom by viewModel.draftCustomState.collectAsStateWithLifecycle()
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val current = slice
    val header =
        if (current == null) {
            stringResource(R.string.amount_loading)
        } else {
            ExpenseFormat.analyticsHeader(current.totalMinor, selection.preset, today, selection.customPeriod)
        }
    TabScaffold(
        title = stringResource(R.string.tab_analytics),
        selectedTab = AppTab.Analytics,
        onTabSelected = onTabSelected,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        subtitle = {
            Text(
                text = header,
                style = typography.bodySecondary,
                color = colors.textSecondary,
            )
        },
    ) {
        Row(
            modifier = Modifier.padding(top = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            FilterChip(
                label = stringResource(R.string.chart_donut),
                selected = chart == AnalyticsChart.Donut,
                onClick = { viewModel.onChart(AnalyticsChart.Donut) },
            )
            FilterChip(
                label = stringResource(R.string.chart_bars),
                selected = chart == AnalyticsChart.Bars,
                onClick = { viewModel.onChart(AnalyticsChart.Bars) },
            )
        }
        PeriodNavigation(
            selection = selection,
            today = today,
            onPrevious = viewModel::onPreviousMonth,
            onNext = viewModel::onNextMonth,
            onOpenPeriods = viewModel::onOpenPeriods,
        )
        when {
            current == null -> {
                Text(
                    text = stringResource(R.string.analytics_loading),
                    style = typography.bodySecondary,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.lg),
                )
            }

            current.isEmpty -> {
                EmptyAnalytics(
                    selection = selection,
                    today = today,
                    onChoosePeriod = viewModel::onOpenPeriods,
                    modifier = Modifier.weight(1f),
                )
            }

            else -> {
                AnalyticsBody(
                    slice = current,
                    chart = chart,
                    onOpenCategory = { row ->
                        onOpenFilteredExpenses(selection.toCategoryFilter(row.categoryId))
                    },
                )
            }
        }
    }
    if (dialog != AnalyticsDialog.None) {
        PeriodSheet(
            today = today,
            draftPreset = draftPreset,
            draftCustom = draftCustom,
            onPreset = viewModel::onDraftPreset,
            onApply = viewModel::onApplyPeriod,
            onDismiss = viewModel::onDismissDialog,
            title = stringResource(R.string.analytics_period_title),
        )
    }
    if (dialog == AnalyticsDialog.CustomStart) {
        ExpenseDatePicker(
            selected = draftCustom?.startInclusive ?: today.minusMonths(1).withDayOfMonth(1),
            today = today,
            onSelected = viewModel::onCustomStart,
            onDismiss = viewModel::onOpenPeriods,
        )
    }
    if (dialog == AnalyticsDialog.CustomEnd) {
        ExpenseDatePicker(
            selected = draftCustom?.endInclusive ?: today,
            today = today,
            onSelected = viewModel::onCustomEnd,
            onDismiss = viewModel::onOpenPeriods,
        )
    }
}

@Composable
private fun AnalyticsBody(
    slice: AnalyticsSlice,
    chart: AnalyticsChart,
    onOpenCategory: (AnalyticsCategoryRow) -> Unit,
) {
    val colors = ExpenseTheme.colors
    val visuals = slice.rows.map { AnalyticsVisualRow(it, it.toVisual(colors).containerColor) }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(top = ExpenseTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(ExpenseTheme.spacing.xs),
    ) {
        item(key = "chart") {
            if (chart == AnalyticsChart.Donut) {
                AnalyticsDonut(
                    rows = visuals,
                    totalLabel = ExpenseFormat.money(slice.totalMinor),
                    onRowClick = onOpenCategory,
                )
            } else {
                AnalyticsBars(rows = visuals, onRowClick = onOpenCategory)
            }
        }
        items(slice.rows, key = { it.categoryId }) { row ->
            LegendRow(row = row, onClick = { onOpenCategory(row) })
        }
    }
}

@Composable
private fun LegendRow(row: AnalyticsCategoryRow, onClick: () -> Unit) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val visual = row.toVisual(colors)
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = MinTapTarget)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        CategoryAvatar(
            glyph = visual.glyph,
            containerColor = visual.containerColor,
            letter = visual.letter,
            size = 32.dp,
        )
        Text(
            text = row.name,
            style = typography.bodyPrimary,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = ExpenseFormat.shareLine(row.amountMinor, row.sharePercent),
            style = typography.bodySecondary,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun PeriodNavigation(
    selection: AnalyticsSelection,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPeriods: () -> Unit,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val canGoNext = selection.canGoNext(today)
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(top = ExpenseTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PeriodArrow(label = "‹", onClick = onPrevious)
        Column(
            modifier =
            Modifier
                .weight(1f)
                .clickable(onClick = onOpenPeriods),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = ExpenseFormat.periodTitle(selection.preset, today, selection.customPeriod),
                style = typography.bodyPrimary,
                color = colors.textPrimary,
            )
            Text(
                text = ExpenseFormat.periodCaption(selection.preset),
                style = typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        PeriodArrow(
            label = "›",
            onClick = onNext,
            enabled = canGoNext,
        )
    }
}

@Composable
private fun PeriodArrow(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val colors = ExpenseTheme.colors
    Box(
        modifier =
        Modifier
            .size(MinTapTarget)
            .alpha(if (enabled) 1f else 0.4f)
            .background(colors.surfaceSubtle, ExpenseTheme.radii.full)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ExpenseTheme.typography.titleSection,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun EmptyAnalytics(
    selection: AnalyticsSelection,
    today: LocalDate,
    onChoosePeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Box(
            modifier =
            Modifier
                .size(96.dp)
                .background(colors.surfaceSubtle, ExpenseTheme.radii.full),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.amount_zero),
                style = typography.displayAmount,
                color = colors.textSecondary,
            )
        }
        Text(
            text =
            stringResource(
                R.string.analytics_empty_title,
                ExpenseFormat.emptyPeriodLabel(selection.preset, today, selection.customPeriod),
            ),
            style = typography.titleSection,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.analytics_empty_description),
            style = typography.bodySecondary,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            label = stringResource(R.string.choose_period),
            onClick = onChoosePeriod,
            modifier = Modifier.fillMaxWidth(0.85f),
        )
    }
}
