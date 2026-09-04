package com.olegbelyanin.expensetracker.ui.expenses

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.domain.expense.ExpenseListItem
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.model.Period
import com.olegbelyanin.expensetracker.ui.components.CategoryGlyphKey
import com.olegbelyanin.expensetracker.ui.components.DateHeader
import com.olegbelyanin.expensetracker.ui.components.ExpenseDatePicker
import com.olegbelyanin.expensetracker.ui.components.ExpenseRow
import com.olegbelyanin.expensetracker.ui.components.ExpenseToast
import com.olegbelyanin.expensetracker.ui.components.FilterChip
import com.olegbelyanin.expensetracker.ui.components.SearchField
import com.olegbelyanin.expensetracker.ui.components.StatePanel
import com.olegbelyanin.expensetracker.ui.components.StatePanelType
import com.olegbelyanin.expensetracker.ui.components.TextAction
import com.olegbelyanin.expensetracker.ui.components.ToastTone
import com.olegbelyanin.expensetracker.ui.components.colorForGlyph
import com.olegbelyanin.expensetracker.ui.components.parseHexColor
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import com.olegbelyanin.expensetracker.ui.navigation.AppTab
import com.olegbelyanin.expensetracker.ui.navigation.TabScaffold
import com.olegbelyanin.expensetracker.ui.theme.BottomNavHeight
import com.olegbelyanin.expensetracker.ui.theme.ExpenseColors
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import com.olegbelyanin.expensetracker.ui.theme.FabSize
import java.time.LocalDate

@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel,
    listState: LazyListState,
    onOpenSettings: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = viewModel.today,
) {
    val slice by viewModel.slice.collectAsStateWithLifecycle()
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    val preset by viewModel.periodPreset.collectAsStateWithLifecycle()
    val customPeriod by viewModel.customPeriodRange.collectAsStateWithLifecycle()
    val selectedCategoryIds by viewModel.selectedCategoryIds.collectAsStateWithLifecycle()
    val selectedLocationId by viewModel.selectedLocationId.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val dialog by viewModel.dialogState.collectAsStateWithLifecycle()
    val draftPreset by viewModel.draftPresetState.collectAsStateWithLifecycle()
    val draftCustom by viewModel.draftCustomState.collectAsStateWithLifecycle()
    val draftCategoryIds by viewModel.draftCategoryIdsState.collectAsStateWithLifecycle()
    val draftLocationName by viewModel.draftLocationNameState.collectAsStateWithLifecycle()
    val locationSuggestions by viewModel.locationSuggestionsState.collectAsStateWithLifecycle()
    val locationFocused by viewModel.locationFocusedState.collectAsStateWithLifecycle()
    val draftSlice by viewModel.draftSlice.collectAsStateWithLifecycle()
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val usedLocations by viewModel.usedLocations.collectAsStateWithLifecycle()
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val current = slice
    val filter = ExpenseListFilter(
        query = query,
        preset = preset,
        customPeriod = customPeriod,
        categoryIds = selectedCategoryIds,
        locationId = selectedLocationId,
    )
    val amountText =
        when {
            current == null -> stringResource(R.string.amount_loading)
            else -> ExpenseFormat.money(current.totalMinor)
        }
    TabScaffold(
        title = stringResource(R.string.tab_expenses),
        selectedTab = AppTab.Expenses,
        onTabSelected = onTabSelected,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        subtitle = {
            Text(
                text = ExpenseFormat.periodSubtitle(preset, today, customPeriod),
                style = typography.labelSmall,
                color = colors.textSecondary,
            )
            Text(
                text = amountText,
                style = typography.displayAmount,
                color = colors.textPrimary,
            )
        },
        showFab = true,
        onFabClick = onAddExpense,
        overlay = {
            toast?.let {
                ExpenseToast(
                    message = stringResource(R.string.expense_saved),
                    tone = ToastTone.Success,
                    actionLabel = stringResource(R.string.undo),
                    onAction = viewModel::onUndoSaved,
                    modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = spacing.md, end = spacing.md, bottom = BottomNavHeight + spacing.sm),
                )
            }
        },
    ) {
        when {
            current == null -> {
                SearchAndChips(
                    filter = filter,
                    categories = activeCategories,
                    locations = usedLocations,
                    today = today,
                    onQueryChange = viewModel::onQueryChange,
                    onPreset = viewModel::onPeriodPreset,
                    onOpenFilters = viewModel::onOpenFilters,
                    onResetFilters = viewModel::onResetFilters,
                )
                StatePanel(
                    type = StatePanelType.Loading,
                    title = stringResource(R.string.expenses_loading_title),
                    description = stringResource(R.string.expenses_loading_description),
                    modifier = Modifier.padding(top = spacing.sm),
                )
            }

            current.isDatabaseEmpty ->
                StatePanel(
                    type = StatePanelType.Empty,
                    title = stringResource(R.string.expenses_empty_title),
                    description = stringResource(R.string.expenses_empty_description),
                    actionLabel = stringResource(R.string.add_expense),
                    onAction = onAddExpense,
                    modifier = Modifier.padding(top = spacing.sm),
                )

            else -> {
                SearchAndChips(
                    filter = filter,
                    categories = activeCategories,
                    locations = usedLocations,
                    today = today,
                    onQueryChange = viewModel::onQueryChange,
                    onPreset = viewModel::onPeriodPreset,
                    onOpenFilters = viewModel::onOpenFilters,
                    onResetFilters = viewModel::onResetFilters,
                )
                if (current.isFilterEmpty) {
                    StatePanel(
                        type = StatePanelType.NoResults,
                        title = stringResource(R.string.expenses_no_results_title),
                        description = stringResource(
                            if (filter.query.isNotBlank() &&
                                filter.categoryIds.isEmpty() &&
                                filter.locationId == null &&
                                filter.preset == ExpensePeriodPreset.ALL
                            ) {
                                R.string.expenses_no_results_search_description
                            } else {
                                R.string.expenses_no_results_description
                            },
                        ),
                        actionLabel = stringResource(R.string.reset_filters),
                        onAction = viewModel::onResetFilters,
                        modifier = Modifier.padding(top = spacing.sm),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        current.groups.forEach { group ->
                            item(key = "header-${group.date}", contentType = "header") {
                                DateHeader(
                                    dateLabel = ExpenseFormat.dayHeader(group.date, group.relative),
                                    groupAmount = ExpenseFormat.money(group.totalMinor),
                                )
                            }
                            items(group.items, key = ExpenseListItem::id, contentType = { "row" }) { item ->
                                val glyph = CategoryGlyphKey.fromStorage(item.categoryIcon)
                                ExpenseRow(
                                    title = item.name,
                                    subtitle =
                                    if (item.locationName.isNullOrBlank()) {
                                        item.categoryName
                                    } else {
                                        "${item.categoryName} · ${item.locationName}"
                                    },
                                    amount = ExpenseFormat.money(item.amountMinor, withMinus = true),
                                    glyph = glyph,
                                    letter = item.categoryName,
                                    containerColor = item.toRowColor(colors, glyph),
                                    onClick = { onOpenExpense(item.id) },
                                )
                            }
                        }
                        item(key = "fab-space") {
                            Spacer(Modifier.height(FabSize + ExpenseTheme.spacing.lg))
                        }
                    }
                }
            }
        }
    }
    if (dialog != ExpensesDialog.None) {
        ExpenseFiltersSheet(
            today = today,
            zoneId = viewModel.zoneId,
            draftPreset = draftPreset,
            categories = activeCategories,
            selectedCategoryIds = draftCategoryIds,
            locationName = draftLocationName,
            locationFocused = locationFocused,
            locationSuggestions = locationSuggestions,
            previewTotalMinor = draftSlice?.totalMinor,
            onPeriod = viewModel::onDraftPeriod,
            onToggleCategory = viewModel::onToggleDraftCategory,
            onLocationQuery = viewModel::onDraftLocationQuery,
            onLocationFocus = viewModel::onDraftLocationFocus,
            onLocationSuggestion = viewModel::onDraftLocationSuggestion,
            onReset = viewModel::onResetDraftFilters,
            onApply = viewModel::onApplyFilters,
            onDismiss = viewModel::onDismissDialog,
        )
    }
    if (dialog == ExpensesDialog.CustomStart) {
        ExpenseDatePicker(
            selected = draftCustom?.startInclusive ?: today.minusMonths(1).withDayOfMonth(1),
            today = today,
            onSelected = viewModel::onCustomStart,
            onDismiss = viewModel::onReturnToFilters,
        )
    }
    if (dialog == ExpensesDialog.CustomEnd) {
        ExpenseDatePicker(
            selected = draftCustom?.endInclusive ?: today,
            today = today,
            onSelected = viewModel::onCustomEnd,
            onDismiss = viewModel::onReturnToFilters,
        )
    }
}

@Composable
private fun ExpenseListItem.toRowColor(colors: ExpenseColors, glyph: CategoryGlyphKey) =
    if (glyph == CategoryGlyphKey.Letter) {
        parseHexColor(categoryColor) ?: colors.other
    } else {
        colors.colorForGlyph(glyph)
    }

@Composable
private fun SearchAndChips(
    filter: ExpenseListFilter,
    categories: List<Category>,
    locations: List<Location>,
    today: LocalDate,
    onQueryChange: (String) -> Unit,
    onPreset: (ExpensePeriodPreset) -> Unit,
    onOpenFilters: () -> Unit,
    onResetFilters: () -> Unit,
) {
    val spacing = ExpenseTheme.spacing
    val compact = ExpenseFilterChrome.isCompact(filter)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        SearchField(query = filter.query, onQueryChange = onQueryChange)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            if (compact) {
                CompactFilterChips(
                    filter = filter,
                    categories = categories,
                    locations = locations,
                    today = today,
                    onOpenFilters = onOpenFilters,
                )
            } else {
                IdleFilterChips(
                    preset = filter.preset,
                    onPreset = onPreset,
                    onOpenFilters = onOpenFilters,
                )
            }
        }
        if (filter.hasActiveConstraints) {
            TextAction(
                label = stringResource(R.string.reset_filters),
                onClick = onResetFilters,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun IdleFilterChips(
    preset: ExpensePeriodPreset,
    onPreset: (ExpensePeriodPreset) -> Unit,
    onOpenFilters: () -> Unit,
) {
    FilterChip(
        label = stringResource(R.string.filter_all),
        selected = preset == ExpensePeriodPreset.ALL,
        onClick = { onPreset(ExpensePeriodPreset.ALL) },
    )
    FilterChip(
        label = stringResource(R.string.filter_month),
        selected = preset == ExpensePeriodPreset.CURRENT_MONTH,
        onClick = { onPreset(ExpensePeriodPreset.CURRENT_MONTH) },
    )
    FilterChip(
        label = stringResource(R.string.filter_category),
        selected = false,
        onClick = onOpenFilters,
    )
}

@Composable
private fun CompactFilterChips(
    filter: ExpenseListFilter,
    categories: List<Category>,
    locations: List<Location>,
    today: LocalDate,
    onOpenFilters: () -> Unit,
) {
    if (filter.preset != ExpensePeriodPreset.ALL) {
        FilterChip(
            label = ExpenseFormat.periodChip(filter.preset, today, filter.customPeriod),
            selected = true,
            onClick = onOpenFilters,
        )
    }
    if (filter.categoryIds.isNotEmpty()) {
        val label =
            if (filter.categoryIds.size == 1) {
                categories.firstOrNull { it.id == filter.categoryIds.first() }?.name
                    ?: stringResource(R.string.filter_category)
            } else {
                ExpenseFormat.categoryCount(filter.categoryIds.size)
            }
        FilterChip(label = label, selected = true, onClick = onOpenFilters)
    }
    filter.locationId?.let { id ->
        FilterChip(
            label = locations.firstOrNull { it.id == id }?.name ?: stringResource(R.string.filter_place),
            selected = true,
            onClick = onOpenFilters,
        )
    }
}
