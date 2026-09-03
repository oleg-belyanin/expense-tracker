package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val background = if (selected) colors.action else colors.surface
    val content = if (selected) colors.onAction else colors.textPrimary
    val border = if (selected) colors.action else colors.border
    Box(
        modifier =
        modifier
            .minimumInteractiveComponentSize()
            .clip(ExpenseTheme.radii.full)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
            Modifier
                .height(36.dp)
                .clip(ExpenseTheme.radii.full)
                .background(background)
                .border(1.dp, border, ExpenseTheme.radii.full)
                .padding(horizontal = spacing.sm, vertical = spacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = typography.labelControl,
                color = content,
            )
        }
    }
}
