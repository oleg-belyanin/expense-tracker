package com.olegbelyanin.expensetracker.ui.expense

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.ui.components.BackAction
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

@Composable
fun ExpenseEditScreen(expenseId: String?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val title =
        if (expenseId == null) {
            stringResource(R.string.expense_new_title)
        } else {
            stringResource(R.string.expense_edit_title)
        }
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
                .padding(start = spacing.xs, end = spacing.md, top = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackAction(onClick = onBack)
            Text(
                text = title,
                style = typography.titleSection,
                color = colors.textPrimary,
            )
        }
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.expense_form_placeholder),
                style = typography.bodySecondary,
                color = colors.textSecondary,
            )
        }
    }
}
