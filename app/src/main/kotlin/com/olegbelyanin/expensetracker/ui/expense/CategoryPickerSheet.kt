package com.olegbelyanin.expensetracker.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.model.CategorizationResult
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.ui.components.BottomSheetSize
import com.olegbelyanin.expensetracker.ui.components.CategoryAvatar
import com.olegbelyanin.expensetracker.ui.components.ExpenseBottomSheet
import com.olegbelyanin.expensetracker.ui.components.SearchField
import com.olegbelyanin.expensetracker.ui.components.toVisual
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import com.olegbelyanin.expensetracker.ui.theme.MinTapTarget

@Composable
fun CategoryPickerSheet(
    name: String,
    query: String,
    selectedId: Long?,
    categories: List<Category>,
    suggestion: CategorizationResult?,
    onQueryChange: (String) -> Unit,
    onSelect: (Category) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
    locationName: String = "",
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val needle = query.trim()
    val visible =
        if (needle.isEmpty()) {
            categories
        } else {
            categories.filter { it.name.contains(needle, ignoreCase = true) }
        }
    val ranked = CategorySuggestionUi.rankedCandidates(suggestion)
    val candidates = ranked.mapNotNull { candidate ->
        visible.find { it.id == candidate.categoryId }?.let { category -> category to candidate.score }
    }
    val rest = visible.filter { category -> candidates.none { it.first.id == category.id } }
        .sortedBy { it.name.lowercase() }
    ExpenseBottomSheet(onDismiss = onDismiss, size = BottomSheetSize.Tall) {
        Text(
            text = stringResource(R.string.category_picker_title),
            style = typography.titleSection,
            color = colors.textPrimary,
        )
        Text(
            text =
            when {
                name.isNotBlank() -> stringResource(R.string.category_picker_hint, name)
                locationName.isNotBlank() && candidates.isNotEmpty() ->
                    stringResource(R.string.category_picker_hint_place, locationName)
                else -> stringResource(R.string.category_picker_hint_empty)
            },
            style = typography.bodySecondary,
            color = colors.textSecondary,
        )
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = stringResource(R.string.category_picker_search),
        )
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (candidates.isNotEmpty() && needle.isEmpty()) {
                SectionLabel(stringResource(R.string.category_picker_best))
                candidates.forEach { (category, score) ->
                    CategoryPickRow(
                        category = category,
                        selected = category.id == selectedId,
                        score = score,
                        onClick = { onSelect(category) },
                    )
                }
            }
            if (rest.isNotEmpty() || needle.isNotEmpty()) {
                SectionLabel(stringResource(R.string.category_picker_all))
                (if (needle.isEmpty()) rest else visible.sortedBy { it.name.lowercase() }).forEach { category ->
                    CategoryPickRow(
                        category = category,
                        selected = category.id == selectedId,
                        score = null,
                        onClick = { onSelect(category) },
                    )
                }
            }
        }
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(ExpenseTheme.radii.full)
                .border(1.5.dp, colors.action, ExpenseTheme.radii.full)
                .clickable(onClick = onCreate),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.create_category),
                style = typography.labelControl,
                color = colors.action,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = ExpenseTheme.typography.labelSmall,
        color = ExpenseTheme.colors.textSecondary,
        modifier = Modifier.padding(top = ExpenseTheme.spacing.xs, bottom = ExpenseTheme.spacing.xxs),
    )
}

@Composable
private fun CategoryPickRow(category: Category, selected: Boolean, score: Double?, onClick: () -> Unit) {
    val colors = ExpenseTheme.colors
    val visual = category.toVisual(colors)
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = MinTapTarget)
            .clip(ExpenseTheme.radii.md)
            .background(if (selected) colors.surfaceSubtle else colors.surface)
            .then(
                if (selected) {
                    Modifier.border(1.dp, colors.action, ExpenseTheme.radii.md)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = ExpenseTheme.spacing.xs, vertical = ExpenseTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ExpenseTheme.spacing.xs),
    ) {
        CategoryAvatar(
            glyph = visual.glyph,
            containerColor = visual.containerColor,
            size = 28.dp,
            letter = visual.letter,
        )
        Text(
            text = category.name,
            style = ExpenseTheme.typography.bodyPrimary,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (score != null && score > 0.0) {
            Text(
                text = ExpenseFormat.scorePercent(score),
                style = ExpenseTheme.typography.bodySecondary,
                color = if (selected) colors.action else colors.textSecondary,
            )
        }
    }
}
