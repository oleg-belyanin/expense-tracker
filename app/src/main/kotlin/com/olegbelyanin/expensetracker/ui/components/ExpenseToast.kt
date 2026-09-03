package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

enum class ToastTone {
    Success,
    Error,
}

@Composable
fun ExpenseToast(message: String, tone: ToastTone, modifier: Modifier = Modifier) {
    val colors = ExpenseTheme.colors
    val container = if (tone == ToastTone.Success) colors.action else colors.danger
    Surface(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = ExpenseTheme.radii.md,
        color = container,
        contentColor = colors.onAction,
        shadowElevation = 6.dp,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = ExpenseTheme.spacing.md, vertical = 16.dp),
            style = ExpenseTheme.typography.bodyPrimary,
        )
    }
}
