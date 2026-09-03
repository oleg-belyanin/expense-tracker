package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

enum class StatePanelType {
    Loading,
    Empty,
    NoResults,
}

@Composable
fun StatePanel(
    type: StatePanelType,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val badge =
        when (type) {
            StatePanelType.Loading -> "↻"
            StatePanelType.Empty -> "₽"
            StatePanelType.NoResults -> "0"
        }
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(vertical = spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Box(
            modifier =
            Modifier
                .size(56.dp)
                .background(colors.surfaceSubtle, ExpenseTheme.radii.full),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badge,
                style = typography.titleSection,
                color = colors.action,
            )
        }
        Text(
            text = title,
            modifier = Modifier.width(280.dp),
            style = typography.titleSection,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            modifier = Modifier.width(280.dp),
            style = typography.bodySecondary,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (type != StatePanelType.Loading && actionLabel != null) {
            Text(
                text = actionLabel,
                modifier =
                Modifier
                    .width(280.dp)
                    .then(
                        if (onAction != null) {
                            Modifier.clickable(onClick = onAction)
                        } else {
                            Modifier
                        },
                    ),
                style = typography.labelControl,
                color = colors.action,
                textAlign = TextAlign.Center,
            )
        }
    }
}
