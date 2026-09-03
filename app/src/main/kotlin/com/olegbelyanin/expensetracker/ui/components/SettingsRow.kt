package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

enum class SettingsRowTone {
    Default,
    Destructive,
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    tone: SettingsRowTone = SettingsRowTone.Default,
    onClick: (() -> Unit)? = null,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val titleColor =
        if (tone == SettingsRowTone.Destructive) colors.danger else colors.textPrimary
    val valueColor =
        if (tone == SettingsRowTone.Destructive) colors.danger else colors.action
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(ExpenseTheme.radii.md)
            .background(colors.surface)
            .border(1.dp, colors.border, ExpenseTheme.radii.md)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = title,
                style = typography.titleSection,
                color = titleColor,
            )
            Text(
                text = subtitle,
                style = typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        if (value != null) {
            Text(
                text = value,
                style = typography.bodySecondary,
                color = valueColor,
            )
        }
    }
}
