package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

enum class ButtonTone {
    Primary,
    Destructive,
}

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ButtonTone = ButtonTone.Primary,
) {
    val colors = ExpenseTheme.colors
    val enabledContainer =
        if (tone == ButtonTone.Destructive) colors.danger else colors.action
    val disabledContainer = colors.surfaceSubtle
    val content =
        if (enabled) colors.onAction else colors.textSecondary
    Button(
        onClick = onClick,
        modifier =
        modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = ExpenseTheme.radii.full,
        colors =
        ButtonDefaults.buttonColors(
            containerColor = enabledContainer,
            contentColor = colors.onAction,
            disabledContainerColor = disabledContainer,
            disabledContentColor = content,
        ),
    ) {
        Text(
            text = label,
            style = ExpenseTheme.typography.labelControl,
        )
    }
}
