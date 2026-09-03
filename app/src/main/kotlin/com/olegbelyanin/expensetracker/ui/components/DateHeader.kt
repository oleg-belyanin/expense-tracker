package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

@Composable
fun DateHeader(dateLabel: String, groupAmount: String, modifier: Modifier = Modifier) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .height(32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dateLabel,
            style = typography.bodySecondary,
            color = colors.textSecondary,
        )
        Text(
            text = groupAmount,
            style = typography.titleSection,
            color = colors.textPrimary,
        )
    }
}
