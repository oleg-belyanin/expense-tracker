package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

@Composable
fun FieldError(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier,
        style = ExpenseTheme.typography.labelSmall,
        color = ExpenseTheme.colors.danger,
    )
}
