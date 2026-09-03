package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import com.olegbelyanin.expensetracker.ui.theme.MinTapTarget

enum class ToastTone {
    Success,
    Error,
}

@Composable
fun ExpenseToast(
    message: String,
    tone: ToastTone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
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
        Row(
            modifier = Modifier.padding(horizontal = ExpenseTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ExpenseTheme.spacing.sm),
        ) {
            Text(
                text = message,
                modifier =
                Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp),
                style = ExpenseTheme.typography.bodySecondary,
            )
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    modifier =
                    Modifier
                        .heightIn(min = MinTapTarget)
                        .clickable(onClick = onAction)
                        .padding(vertical = 16.dp),
                    style = ExpenseTheme.typography.labelControl,
                )
            }
        }
    }
}
