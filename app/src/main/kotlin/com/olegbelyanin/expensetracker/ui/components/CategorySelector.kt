package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

enum class CategorySelectorState {
    Auto,
    Manual,
}

@Composable
fun CategorySelector(
    name: String,
    glyph: CategoryGlyphKey,
    state: CategorySelectorState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    letter: String = "",
    sourceLabel: String? = null,
    containerColor: Color? = null,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val defaultLabel =
        if (state == CategorySelectorState.Auto) {
            stringResource(R.string.category_source_auto)
        } else {
            stringResource(R.string.category_source_manual)
        }
    val subtitle = sourceLabel ?: defaultLabel
    val autofilled = state == CategorySelectorState.Auto
    val fieldLabel =
        if (autofilled) {
            stringResource(R.string.field_category_auto)
        } else {
            stringResource(R.string.field_category)
        }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Text(
            text = fieldLabel,
            style = typography.labelSmall,
            color = if (autofilled) colors.action else colors.textSecondary,
        )
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(ExpenseTheme.radii.md)
                .background(colors.surface)
                .border(
                    width = if (autofilled) 1.5.dp else 1.dp,
                    color = if (autofilled) colors.action else colors.border,
                    shape = ExpenseTheme.radii.md,
                )
                .clickable(onClick = onClick)
                .padding(start = spacing.xs, end = spacing.sm, top = spacing.xs, bottom = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            CategoryAvatar(
                glyph = glyph,
                containerColor = containerColor ?: colors.colorForGlyph(glyph),
                letter = letter,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = name,
                    style = typography.bodyPrimary,
                    color = colors.textPrimary,
                )
                Text(
                    text = subtitle,
                    style = typography.bodySecondary,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
