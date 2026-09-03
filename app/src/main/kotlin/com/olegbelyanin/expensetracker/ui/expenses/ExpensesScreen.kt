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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.domain.expense.ExpenseListItem
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.model.Period
import com.olegbelyanin.expensetracker.ui.components.BottomSheetSize
import com.olegbelyanin.expensetracker.ui.components.CategoryGlyphKey
import com.olegbelyanin.expensetracker.ui.components.DateHeader
import com.olegbelyanin.expensetracker.ui.components.ExpenseBottomSheet
import com.olegbelyanin.expensetracker.ui.components.ExpenseDatePicker
import com.olegbelyanin.expensetracker.ui.components.ExpenseRow
import com.olegbelyanin.expensetracker.ui.components.ExpenseToast
import com.olegbelyanin.expensetracker.ui.components.FilterChip
import com.olegbelyanin.expensetracker.ui.components.PeriodSheet
import com.olegbelyanin.expensetracker.ui.components.SearchField
import com.olegbelyanin.expensetracker.ui.components.SelectionRow
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
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val usedLocations by viewModel.usedLocations.collectAsStateWithLifecycle()
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val current = slice
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
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    preset = preset,
                    customPeriod = customPeriod,
                    selectedCategoryIds = selectedCategoryIds,
                    selectedLocationId = selectedLocationId,
                    categories = activeCategories,
                    locations = usedLocations,
                    today = today,
                    onPreset = viewModel::onPeriodPreset,
                    onOpenPeriodFilter = viewModel::onOpenPeriodFilter,
                    onOpenCategoryFilter = viewModel::onOpenCategoryFilter,
                    onOpenLocationFilter = viewModel::onOpenLocationFilter,
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
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    preset = preset,
                    customPeriod = customPeriod,
                    selectedCategoryIds = selectedCategoryIds,
                    selectedLocationId = selectedLocationId,
                    categories = activeCategories,
                    locations = usedLocations,
                    today = today,
                    onPreset = viewModel::onPeriodPreset,
                    onOpenPeriodFilter = viewModel::onOpenPeriodFilter,
                    onOpenCategoryFilter = viewModel::onOpenCategoryFilter,
                    onOpenLocationFilter = viewModel::onOpenLocationFilter,
                    onResetFilters = viewModel::onResetFilters,
                )
                if (current.isFilterEmpty) {
                    StatePanel(
                        type = StatePanelType.NoResults,
                        title = stringResource(R.string.expenses_no_results_title),
                        description = stringResource(R.string.expenses_no_results_description),
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
                            Spacer(Modifier.height(FabSize + spacing.lg))
                        }
                    }
                }
            }
        }
    }
    if (dialog == ExpensesDialog.Categories) {
        CategoryFilterSheet(
            categories = activeCategories,
            selectedIds = selectedCategoryIds,
            onToggle = viewModel::onToggleCategory,
            onDismiss = viewModel::onDismissDialog,
        )
    }
    if (dialog == ExpensesDialog.Locations) {
        LocationFilterSheet(
            locations = usedLocations,
            selectedId = selectedLocationId,
            onSelect = viewModel::onSelectLocation,
            onDismiss = viewModel::onDismissDialog,
        )
    }
    if (dialog == ExpensesDialog.Periods ||
        dialog == ExpensesDialog.CustomStart ||
        dialog == ExpensesDialog.CustomEnd
    ) {
        PeriodSheet(
            today = today,
            draftPreset = draftPreset,
            draftCustom = draftCustom,
            onPreset = viewModel::onDraftPreset,
            onApply = viewModel::onApplyPeriod,
            onDismiss = viewModel::onDismissDialog,
        )
    }
    if (dialog == ExpensesDialog.CustomStart) {
        ExpenseDatePicker(
            selected = draftCustom?.startInclusive ?: today.minusMonths(1).withDayOfMonth(1),
            today = today,
            onSelected = viewModel::onCustomStart,
            onDismiss = viewModel::onOpenPeriodFilter,
        )
    }
    if (dialog == ExpensesDialog.CustomEnd) {
        ExpenseDatePicker(
            selected = draftCustom?.endInclusive ?: today,
            today = today,
            onSelected = viewModel::onCustomEnd,
            onDismiss = viewModel::onOpenPeriodFilter,
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
    query: String,
    onQueryChange: (String) -> Unit,
    preset: ExpensePeriodPreset,
    customPeriod: Period?,
    selectedCategoryIds: Set<Long>,
    selectedLocationId: Long?,
    categories: List<Category>,
    locations: List<Location>,
    today: LocalDate,
    onPreset: (ExpensePeriodPreset) -> Unit,
    onOpenPeriodFilter: () -> Unit,
    onOpenCategoryFilter: () -> Unit,
    onOpenLocationFilter: () -> Unit,
    onResetFilters: () -> Unit,
) {
    val spacing = ExpenseTheme.spacing
    val extraPeriod = preset != ExpensePeriodPreset.ALL && preset != ExpensePeriodPreset.CURRENT_MONTH
    val hasFilters =
        query.isNotBlank() ||
            preset != ExpensePeriodPreset.ALL ||
            customPeriod != null ||
            selectedCategoryIds.isNotEmpty() ||
            selectedLocationId != null
    val categoryLabel =
        when {
            selectedCategoryIds.size == 1 ->
                categories.firstOrNull { it.id == selectedCategoryIds.first() }?.name
                    ?: stringResource(R.string.filter_category)

            selectedCategoryIds.size > 1 -> ExpenseFormat.categoryCount(selectedCategoryIds.size)

            else -> stringResource(R.string.filter_category)
        }
    val placeLabel =
        selectedLocationId?.let { id -> locations.firstOrNull { it.id == id }?.name }
            ?: stringResource(R.string.filter_place)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        SearchField(query = query, onQueryChange = onQueryChange)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            FilterChip(
                label = stringResource(R.string.filter_all),
                selected = preset == ExpensePeriodPreset.ALL && customPeriod == null,
                onClick = { onPreset(ExpensePeriodPreset.ALL) },
            )
            FilterChip(
                label = stringResource(R.string.filter_month),
                selected = preset == ExpensePeriodPreset.CURRENT_MONTH,
                onClick = { onPreset(ExpensePeriodPreset.CURRENT_MONTH) },
            )
            FilterChip(
                label = if (extraPeriod) {
                    ExpenseFormat.periodChip(preset, today, customPeriod)
                } else {
                    stringResource(R.string.filter_period)
                },
                selected = extraPeriod,
                onClick = onOpenPeriodFilter,
            )
            FilterChip(
                label = categoryLabel,
                selected = selectedCategoryIds.isNotEmpty(),
                onClick = onOpenCategoryFilter,
            )
            FilterChip(
                label = placeLabel,
                selected = selectedLocationId != null,
                onClick = onOpenLocationFilter,
            )
        }
        if (hasFilters) {
            TextAction(
                label = stringResource(R.string.reset_filters),
                onClick = onResetFilters,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun CategoryFilterSheet(
    categories: List<Category>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ExpenseTheme.colors
    ExpenseBottomSheet(onDismiss = onDismiss, size = BottomSheetSize.Tall) {
        Text(
            text = stringResource(R.string.filter_category_title),
            style = ExpenseTheme.typography.titleSection,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.filter_category_hint),
            style = ExpenseTheme.typography.bodySecondary,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = ExpenseTheme.spacing.xs),
        )
        categories.forEach { category ->
            SelectionRow(
                title = category.name,
                subtitle = if (category.id in selectedIds) {
                    stringResource(R.string.filter_category_selected)
                } else {
                    stringResource(R.string.filter_category_unselected)
                },
                selected = category.id in selectedIds,
                onClick = { onToggle(category.id) },
                modifier = Modifier.padding(bottom = ExpenseTheme.spacing.xs),
            )
        }
    }
}

@Composable
private fun LocationFilterSheet(
    locations: List<Location>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ExpenseTheme.colors
    ExpenseBottomSheet(onDismiss = onDismiss, size = BottomSheetSize.Tall) {
        Text(
            text = stringResource(R.string.filter_place_title),
            style = ExpenseTheme.typography.titleSection,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(
                if (locations.isEmpty()) R.string.filter_place_empty else R.string.filter_place_hint,
            ),
            style = ExpenseTheme.typography.bodySecondary,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = ExpenseTheme.spacing.xs),
        )
        locations.forEach { location ->
            SelectionRow(
                title = location.name,
                subtitle = if (location.id == selectedId) {
                    stringResource(R.string.filter_place_selected)
                } else {
                    stringResource(R.string.filter_place_unselected)
                },
                selected = location.id == selectedId,
                onClick = { onSelect(location.id) },
                modifier = Modifier.padding(bottom = ExpenseTheme.spacing.xs),
            )
        }
    }
}
