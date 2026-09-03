package com.olegbelyanin.expensetracker.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.ui.components.FilterChip
import com.olegbelyanin.expensetracker.ui.components.PrimaryButton
import com.olegbelyanin.expensetracker.ui.navigation.AppTab
import com.olegbelyanin.expensetracker.ui.navigation.TabScaffold
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import com.olegbelyanin.expensetracker.ui.theme.MinTapTarget
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AnalyticsScreen(onOpenSettings: () -> Unit, onTabSelected: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val month = YearMonth.now()
    val monthLabel = formatMonth(month)
    TabScaffold(
        title = stringResource(R.string.tab_analytics),
        selectedTab = AppTab.Analytics,
        onTabSelected = onTabSelected,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        subtitle = {
            Text(
                text = stringResource(R.string.analytics_total, monthLabel),
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
                selected = true,
                onClick = {},
            )
            FilterChip(
                label = stringResource(R.string.chart_bars),
                selected = false,
                onClick = {},
            )
        }
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeriodArrow(label = "‹", onClick = {})
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = monthLabel.replaceFirstChar { it.titlecase(Locale("ru")) },
                    style = typography.bodyPrimary,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.period_current_month),
                    style = typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
            PeriodArrow(label = "›", onClick = {})
        }
        Column(
            modifier =
            Modifier
                .weight(1f)
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
                text = stringResource(R.string.analytics_empty_title, month.monthLabel()),
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
                onClick = {},
                modifier = Modifier.fillMaxWidth(0.85f),
            )
        }
    }
}

@Composable
private fun PeriodArrow(label: String, onClick: () -> Unit) {
    val colors = ExpenseTheme.colors
    Box(
        modifier =
        Modifier
            .size(MinTapTarget)
            .background(colors.surfaceSubtle, ExpenseTheme.radii.full)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ExpenseTheme.typography.titleSection,
            color = colors.textPrimary,
        )
    }
}

private fun formatMonth(month: YearMonth): String {
    val name = month.monthLabel()
    return "$name ${month.year}"
}

private fun YearMonth.monthLabel(): String = month
    .getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))
    .lowercase(Locale("ru"))
