package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Period
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import java.time.LocalDate

@Composable
fun PeriodSheet(
    today: LocalDate,
    draftPreset: ExpensePeriodPreset,
    draftCustom: Period?,
    onPreset: (ExpensePeriodPreset) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.filter_period_title),
) {
    val colors = ExpenseTheme.colors
    val spacing = ExpenseTheme.spacing
    ExpenseBottomSheet(onDismiss = onDismiss, size = BottomSheetSize.Tall, modifier = modifier) {
        Text(
            text = title,
            style = ExpenseTheme.typography.titleSection,
            color = colors.textPrimary,
            modifier = Modifier.padding(bottom = spacing.xs),
        )
        ExpensePeriodPreset.entries.forEach { preset ->
            SelectionRow(
                title = ExpenseFormat.periodCaption(preset),
                subtitle = ExpenseFormat.periodDetail(preset, today, draftCustom),
                selected = draftPreset == preset,
                onClick = { onPreset(preset) },
                modifier = Modifier.padding(bottom = spacing.xs),
            )
        }
        PrimaryButton(
            label = stringResource(R.string.apply),
            onClick = onApply,
        )
    }
}
