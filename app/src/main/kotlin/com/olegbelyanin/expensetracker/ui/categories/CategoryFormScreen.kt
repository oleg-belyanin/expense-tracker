package com.olegbelyanin.expensetracker.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.domain.category.CategoryNameError
import com.olegbelyanin.expensetracker.model.CategoryIcons
import com.olegbelyanin.expensetracker.model.CategoryPalette
import com.olegbelyanin.expensetracker.ui.components.CategoryGlyph
import com.olegbelyanin.expensetracker.ui.components.CategoryGlyphKey
import com.olegbelyanin.expensetracker.ui.components.FormField
import com.olegbelyanin.expensetracker.ui.components.PrimaryButton
import com.olegbelyanin.expensetracker.ui.components.TextAction
import com.olegbelyanin.expensetracker.ui.components.parseHexColor
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import com.olegbelyanin.expensetracker.ui.theme.MinTapTarget

@Composable
fun CategoryFormScreen(
    viewModel: CategoryFormViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.missing) {
        if (state.missing) onBack()
    }
    if (!state.isReady || state.missing) return
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val draft = state.draft
    val nameError = viewModel.nameError()
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = spacing.md, vertical = spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.category_form_title),
                style = typography.headlineScreen,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            TextAction(label = stringResource(R.string.cancel), onClick = onBack)
        }
        Text(
            text =
            if (nameError != null) {
                stringResource(R.string.category_form_subtitle_error)
            } else {
                stringResource(R.string.category_form_subtitle)
            },
            style = typography.bodySecondary,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.xxs, bottom = spacing.sm),
        )
        Column(
            modifier =
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            FormField(
                label = stringResource(R.string.field_category_name_short),
                value = draft.name,
                onValueChange = viewModel::onNameChange,
                error = nameError?.toMessage(),
            )
            Text(
                text = stringResource(R.string.category_form_color),
                style = typography.titleSection,
                color = colors.textPrimary,
            )
            ColorSwatchRow(selected = draft.color, onSelect = viewModel::onColor)
            Text(
                text = stringResource(R.string.category_form_icon),
                style = typography.titleSection,
                color = colors.textPrimary,
            )
            IconCatalog(
                selected = draft.icon,
                letter = draft.name,
                onSelect = viewModel::onIcon,
            )
            Text(
                text = formHint(draft.name, state.suggestedIcon),
                style = typography.bodySecondary,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(spacing.sm))
        }
        PrimaryButton(
            label = stringResource(R.string.category_form_save),
            onClick = { viewModel.onSave(onSaved) },
            enabled = viewModel.canSave(),
        )
    }
}

@Composable
private fun ColorSwatchRow(selected: String, onSelect: (String) -> Unit) {
    val colors = ExpenseTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CategoryPalette.swatches.forEach { hex ->
            val isSelected = hex.equals(selected, ignoreCase = true)
            Box(
                modifier =
                Modifier
                    .size(MinTapTarget)
                    .clip(CircleShape)
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                    Modifier
                        .size(40.dp)
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, colors.action, CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .padding(if (isSelected) 3.dp else 0.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(hex) ?: colors.other),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconCatalog(selected: String, letter: String, onSelect: (String) -> Unit) {
    val colors = ExpenseTheme.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ExpenseTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(ExpenseTheme.spacing.xs),
    ) {
        CategoryIcons.catalog.forEach { key ->
            val isSelected = key == selected
            val glyph = CategoryGlyphKey.fromStorage(key)
            Box(
                modifier =
                Modifier
                    .size(MinTapTarget)
                    .clip(ExpenseTheme.radii.md)
                    .background(if (isSelected) colors.action else colors.surface)
                    .border(
                        1.dp,
                        if (isSelected) colors.action else colors.border,
                        ExpenseTheme.radii.md,
                    )
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                CategoryGlyph(
                    key = glyph,
                    size = 24.dp,
                    letter = letter,
                    contentColor = if (isSelected) colors.onAction else colors.onCategory,
                )
            }
        }
    }
}

@Composable
private fun formHint(name: String, suggestedIcon: String): String {
    val trimmed = name.trim()
    return when {
        trimmed.none { it.isLetterOrDigit() } -> stringResource(R.string.category_form_hint_empty)

        suggestedIcon == CategoryIcons.LETTER ->
            stringResource(R.string.category_form_hint_letter, trimmed)

        else -> stringResource(R.string.category_form_hint_suggested, trimmed, suggestedIcon)
    }
}

@Composable
private fun CategoryNameError.toMessage(): String = when (this) {
    CategoryNameError.EMPTY -> stringResource(R.string.category_name_error_empty)
    CategoryNameError.DUPLICATE -> stringResource(R.string.category_name_error_duplicate)
}
