package com.olegbelyanin.expensetracker.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.ui.components.BottomSheetSize
import com.olegbelyanin.expensetracker.ui.components.ExpenseBottomSheet
import com.olegbelyanin.expensetracker.ui.components.FilterChip
import com.olegbelyanin.expensetracker.ui.components.PlaceAutocomplete
import com.olegbelyanin.expensetracker.ui.components.PlaceSuggestion
import com.olegbelyanin.expensetracker.ui.components.PrimaryButton
import com.olegbelyanin.expensetracker.ui.components.TextAction
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ExpenseFiltersSheet(
    today: LocalDate,
    zoneId: ZoneId,
    draftPreset: ExpensePeriodPreset,
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    locationName: String,
    locationFocused: Boolean,
    locationSuggestions: List<Location>,
    previewTotalMinor: Long?,
    onPeriod: (ExpensePeriodPreset) -> Unit,
    onToggleCategory: (Long) -> Unit,
    onLocationQuery: (String) -> Unit,
    onLocationFocus: (Boolean) -> Unit,
    onLocationSuggestion: (Location) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    var categoriesExpanded by remember { mutableStateOf(false) }
    val colors = ExpenseTheme.colors
    val spacing = ExpenseTheme.spacing
    val visibleCategories = ExpenseFilterChrome.visibleCategories(
        categories = categories,
        selectedIds = selectedCategoryIds,
        expanded = categoriesExpanded,
    )
    val showMore = ExpenseFilterChrome.showsMoreCategories(categories, categoriesExpanded)
    val amount = previewTotalMinor?.let { ExpenseFormat.money(it) } ?: stringResource(R.string.amount_loading)
    ExpenseBottomSheet(onDismiss = onDismiss, size = BottomSheetSize.Tall) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = stringResource(R.string.filters_title),
                style = ExpenseTheme.typography.titleSection,
                color = colors.textPrimary,
            )
            FilterSection(title = stringResource(R.string.filter_period)) {
                FilterChip(
                    label = ExpenseFormat.periodChip(ExpensePeriodPreset.CURRENT_MONTH, today),
                    selected = draftPreset == ExpensePeriodPreset.CURRENT_MONTH,
                    onClick = { onPeriod(ExpensePeriodPreset.CURRENT_MONTH) },
                )
                FilterChip(
                    label = stringResource(R.string.filter_year),
                    selected = draftPreset == ExpensePeriodPreset.YEAR,
                    onClick = { onPeriod(ExpensePeriodPreset.YEAR) },
                )
                FilterChip(
                    label = stringResource(R.string.filter_range),
                    selected = draftPreset == ExpensePeriodPreset.CUSTOM ||
                        draftPreset == ExpensePeriodPreset.PREVIOUS_MONTH,
                    onClick = { onPeriod(ExpensePeriodPreset.CUSTOM) },
                )
            }
            FilterSection(title = stringResource(R.string.filter_category_title)) {
                visibleCategories.forEach { category ->
                    FilterChip(
                        label = category.name,
                        selected = category.id in selectedCategoryIds,
                        onClick = { onToggleCategory(category.id) },
                    )
                }
                if (showMore) {
                    FilterChip(
                        label = stringResource(R.string.filter_more),
                        selected = false,
                        onClick = { categoriesExpanded = true },
                    )
                }
            }
            PlaceAutocomplete(
                query = locationName,
                onQueryChange = onLocationQuery,
                suggestions =
                if (locationFocused) {
                    locationSuggestions.map { location ->
                        PlaceSuggestion(
                            name = location.name,
                            detail = ExpenseFormat.locationDetail(location, today, zoneId),
                        )
                    }
                } else {
                    emptyList()
                },
                onSuggestionClick = { suggestion ->
                    locationSuggestions.firstOrNull { it.name == suggestion.name }
                        ?.let(onLocationSuggestion)
                },
                onFocusChange = onLocationFocus,
            )
            TextAction(
                label = stringResource(R.string.filter_reset_all),
                onClick = onReset,
            )
            Text(
                text = stringResource(R.string.filter_combine_hint),
                style = ExpenseTheme.typography.bodySecondary,
                color = colors.textSecondary,
            )
            PrimaryButton(
                label = stringResource(R.string.filter_show, amount),
                onClick = onApply,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(title: String, content: @Composable FlowRowScope.() -> Unit) {
    val spacing = ExpenseTheme.spacing
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = title,
            style = ExpenseTheme.typography.labelControl,
            color = ExpenseTheme.colors.textPrimary,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            content = content,
        )
    }
}
