package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

data class PlaceSuggestion(val name: String, val detail: String)

@Composable
fun PlaceAutocomplete(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<PlaceSuggestion>,
    onSuggestionClick: (PlaceSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        FormField(
            label = stringResource(R.string.field_place),
            value = query,
            onValueChange = onQueryChange,
            kind = FormFieldKind.Text,
            error = error,
        )
        if (suggestions.isNotEmpty()) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, ExpenseTheme.radii.md)
                    .clip(ExpenseTheme.radii.md)
                    .background(colors.surface)
                    .border(1.dp, colors.border, ExpenseTheme.radii.md),
            ) {
                suggestions.forEachIndexed { index, suggestion ->
                    val highlighted = index == 0
                    Column(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(if (highlighted) colors.surfaceSubtle else colors.surface)
                            .clickable { onSuggestionClick(suggestion) }
                            .padding(horizontal = spacing.sm, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = suggestion.name,
                            style = typography.bodyPrimary,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = suggestion.detail,
                            style = typography.labelSmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
