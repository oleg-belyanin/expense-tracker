package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

@Composable
fun DestructiveDialog(
    title: String,
    description: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmTone: ButtonTone = ButtonTone.Destructive,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.width(328.dp),
            shape = ExpenseTheme.radii.lg,
            color = colors.surface,
        ) {
            Column(
                modifier =
                Modifier.padding(
                    horizontal = spacing.md,
                    vertical = spacing.lg,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = title,
                    style = typography.titleSection,
                    color = colors.textPrimary,
                )
                Text(
                    text = description,
                    style = typography.bodySecondary,
                    color = colors.textSecondary,
                )
                PrimaryButton(
                    label = confirmLabel,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    tone = confirmTone,
                )
                PrimaryButton(
                    label = cancelLabel,
                    onClick = onDismiss,
                    tone = ButtonTone.Subtle,
                )
            }
        }
    }
}
