package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.background
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

@Composable
fun ExpenseRow(
    title: String,
    subtitle: String,
    amount: String,
    glyph: CategoryGlyphKey,
    modifier: Modifier = Modifier,
    letter: String = "",
    onClick: (() -> Unit)? = null,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(ExpenseTheme.radii.md)
            .background(colors.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        CategoryAvatar(
            glyph = glyph,
            containerColor = colors.colorForGlyph(glyph),
            letter = letter,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = typography.bodyPrimary,
                color = colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = typography.bodySecondary,
                color = colors.textSecondary,
            )
        }
        Text(
            text = amount,
            style = typography.titleSection,
            color = colors.textPrimary,
        )
    }
}
