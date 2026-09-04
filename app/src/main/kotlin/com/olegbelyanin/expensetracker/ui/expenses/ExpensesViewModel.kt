package com.olegbelyanin.expensetracker.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.LocationRepository
import com.olegbelyanin.expensetracker.domain.expense.DeleteExpenseUseCase
import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.domain.expense.ExpenseListSlice
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodResolver
import com.olegbelyanin.expensetracker.domain.expense.ObserveExpenseListUseCase
import com.olegbelyanin.expensetracker.domain.expense.SuggestLocationsUseCase
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.model.Period
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

sealed interface ExpensesToast {
    data class Saved(val expenseId: String) : ExpensesToast

    data object UndoFailed : ExpensesToast
}

enum class ExpensesDialog {
    None,
    Filters,
    CustomStart,
    CustomEnd,
}

class ExpensesViewModel(
    private val observeList: ObserveExpenseListUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
    private val suggestLocations: SuggestLocationsUseCase,
    categories: CategoryRepository,
    locations: LocationRepository,
    clock: Clock,
    val zoneId: ZoneId,
) : ViewModel() {
    val today: LocalDate = LocalDate.now(clock.withZone(zoneId))

    private val query = MutableStateFlow("")
    private val preset = MutableStateFlow(ExpensePeriodPreset.ALL)
    private val customPeriod = MutableStateFlow<Period?>(null)
    private val categoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val locationId = MutableStateFlow<Long?>(null)
    private val _toast = MutableStateFlow<ExpensesToast?>(null)
    private val dialog = MutableStateFlow(ExpensesDialog.None)
    private val draftPreset = MutableStateFlow(ExpensePeriodPreset.ALL)
    private val draftCustom = MutableStateFlow<Period?>(null)
    private val draftCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val draftLocationId = MutableStateFlow<Long?>(null)
    private val draftLocationName = MutableStateFlow("")
    private val locationSuggestions = MutableStateFlow<List<Location>>(emptyList())
    private val locationFocused = MutableStateFlow(false)
    private var toastJob: Job? = null
    private var locationJob: Job? = null

    val queryText: StateFlow<String> = query.asStateFlow()
    val periodPreset: StateFlow<ExpensePeriodPreset> = preset.asStateFlow()
    val customPeriodRange: StateFlow<Period?> = customPeriod.asStateFlow()
    val selectedCategoryIds: StateFlow<Set<Long>> = categoryIds.asStateFlow()
    val selectedLocationId: StateFlow<Long?> = locationId.asStateFlow()
    val toast: StateFlow<ExpensesToast?> = _toast.asStateFlow()
    val dialogState: StateFlow<ExpensesDialog> = dialog.asStateFlow()
    val draftPresetState: StateFlow<ExpensePeriodPreset> = draftPreset.asStateFlow()
    val draftCustomState: StateFlow<Period?> = draftCustom.asStateFlow()
    val draftCategoryIdsState: StateFlow<Set<Long>> = draftCategoryIds.asStateFlow()
    val draftLocationNameState: StateFlow<String> = draftLocationName.asStateFlow()
    val locationSuggestionsState: StateFlow<List<Location>> = locationSuggestions.asStateFlow()
    val locationFocusedState: StateFlow<Boolean> = locationFocused.asStateFlow()

    val activeCategories: StateFlow<List<Category>> =
        categories.observeActiveCategories().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val usedLocations: StateFlow<List<Location>> =
        locations.observeUsed().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    private val appliedFilter =
        combine(query, preset, customPeriod, categoryIds, locationId) { text, period, custom, ids, place ->
            ExpenseListFilter(
                query = text,
                preset = period,
                customPeriod = custom,
                categoryIds = ids,
                locationId = place,
            )
        }

    private val draftFilter =
        combine(
            query,
            draftPreset,
            draftCustom,
            draftCategoryIds,
            draftLocationId,
        ) { text, period, custom, ids, place ->
            ExpenseListFilter(
                query = text,
                preset = period,
                customPeriod = custom,
                categoryIds = ids,
                locationId = place,
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val slice: StateFlow<ExpenseListSlice?> =
        appliedFilter.flatMapLatest { observeList.observe(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val draftSlice: StateFlow<ExpenseListSlice?> =
        combine(dialog, draftFilter) { current, filter ->
            current.takeUnless { it == ExpensesDialog.None }?.let { filter }
        }.flatMapLatest { filter ->
            if (filter == null) flowOf(null) else observeList.observe(filter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onPeriodPreset(value: ExpensePeriodPreset) {
        preset.value = value
        if (value != ExpensePeriodPreset.CUSTOM) {
            customPeriod.value = null
        }
    }

    fun applyFromAnalytics(filter: ExpenseListFilter) {
        query.value = ""
        preset.value = filter.preset
        customPeriod.value = filter.customPeriod
        categoryIds.value = filter.categoryIds
        locationId.value = filter.locationId
    }

    fun onOpenFilters() {
        copyAppliedToDraft()
        dialog.value = ExpensesDialog.Filters
    }

    fun onOpenPeriodFilters() {
        copyAppliedToDraft()
        if (draftPreset.value == ExpensePeriodPreset.ALL) {
            draftPreset.value = ExpensePeriodPreset.CURRENT_MONTH
        }
        dialog.value = ExpensesDialog.Filters
    }

    fun onReturnToFilters() {
        dialog.value = ExpensesDialog.Filters
    }

    fun onDismissDialog() {
        dialog.value = ExpensesDialog.None
        locationFocused.value = false
        locationSuggestions.value = emptyList()
    }

    fun onDraftPeriod(value: ExpensePeriodPreset) {
        draftPreset.value = value
        if (value == ExpensePeriodPreset.CUSTOM) {
            if (draftCustom.value == null) {
                draftCustom.value = ExpensePeriodResolver.defaultCustom(today)
            }
            dialog.value = ExpensesDialog.CustomStart
        } else {
            draftCustom.value = null
        }
    }

    fun onCustomStart(date: LocalDate) {
        val end = draftCustom.value?.endInclusive ?: today
        draftCustom.value = Period.of(date, end)
        draftPreset.value = ExpensePeriodPreset.CUSTOM
        dialog.value = ExpensesDialog.CustomEnd
    }

    fun onCustomEnd(date: LocalDate) {
        val start = draftCustom.value?.startInclusive ?: today
        draftCustom.value = Period.of(start, date)
        draftPreset.value = ExpensePeriodPreset.CUSTOM
        dialog.value = ExpensesDialog.Filters
    }

    fun onToggleDraftCategory(id: Long) {
        val next = draftCategoryIds.value.toMutableSet()
        if (!next.add(id)) {
            next.remove(id)
        }
        draftCategoryIds.value = next
    }

    fun onDraftLocationQuery(value: String) {
        draftLocationName.value = value
        val selected = usedLocations.value.firstOrNull { it.id == draftLocationId.value }
        if (selected == null || !selected.name.equals(value.trim(), ignoreCase = true)) {
            draftLocationId.value = null
        }
        locationFocused.value = true
        scheduleLocations(value)
    }

    fun onDraftLocationFocus(focused: Boolean) {
        locationFocused.value = focused
        if (focused) {
            scheduleLocations(draftLocationName.value)
        }
    }

    fun onDraftLocationSuggestion(location: Location) {
        draftLocationName.value = location.name
        draftLocationId.value = location.id
        locationSuggestions.value = emptyList()
        locationFocused.value = false
    }

    fun onResetDraftFilters() {
        draftPreset.value = ExpensePeriodPreset.ALL
        draftCustom.value = null
        draftCategoryIds.value = emptySet()
        draftLocationId.value = null
        draftLocationName.value = ""
        locationSuggestions.value = emptyList()
    }

    fun onApplyFilters() {
        val resolved = ExpenseFilterChrome.resolveLocationId(
            name = draftLocationName.value,
            selectedId = draftLocationId.value,
            locations = usedLocations.value,
        )
        preset.value = draftPreset.value
        customPeriod.value = if (draftPreset.value == ExpensePeriodPreset.CUSTOM) draftCustom.value else null
        categoryIds.value = draftCategoryIds.value
        locationId.value = resolved
        onDismissDialog()
    }

    fun onResetFilters() {
        query.value = ""
        preset.value = ExpensePeriodPreset.ALL
        customPeriod.value = null
        categoryIds.value = emptySet()
        locationId.value = null
        onResetDraftFilters()
    }

    fun onExpenseSaved(expenseId: String) {
        showToast(ExpensesToast.Saved(expenseId))
    }

    fun onUndoSaved() {
        val id = (_toast.value as? ExpensesToast.Saved)?.expenseId ?: return
        toastJob?.cancel()
        _toast.value = null
        viewModelScope.launch {
            try {
                deleteExpense(id)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showToast(ExpensesToast.UndoFailed)
            }
        }
    }

    private fun showToast(toast: ExpensesToast) {
        _toast.value = toast
        toastJob?.cancel()
        toastJob =
            viewModelScope.launch {
                delay(4_000)
                if (_toast.value == toast) {
                    _toast.value = null
                }
            }
    }

    private fun copyAppliedToDraft() {
        draftPreset.value = preset.value
        draftCustom.value = when (preset.value) {
            ExpensePeriodPreset.CUSTOM -> customPeriod.value ?: ExpensePeriodResolver.defaultCustom(today)
            else -> customPeriod.value
        }
        draftCategoryIds.value = categoryIds.value
        draftLocationId.value = locationId.value
        draftLocationName.value = locationId.value
            ?.let { id -> usedLocations.value.firstOrNull { it.id == id }?.name }
            .orEmpty()
        locationSuggestions.value = emptyList()
        locationFocused.value = false
    }

    private fun scheduleLocations(value: String) {
        locationJob?.cancel()
        locationJob =
            viewModelScope.launch {
                delay(120)
                locationSuggestions.value = suggestLocations(value)
            }
    }

    companion object {
        fun factory(
            observeList: ObserveExpenseListUseCase,
            deleteExpense: DeleteExpenseUseCase,
            suggestLocations: SuggestLocationsUseCase,
            categories: CategoryRepository,
            locations: LocationRepository,
            clock: Clock,
            zoneId: ZoneId,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ExpensesViewModel(
                    observeList,
                    deleteExpense,
                    suggestLocations,
                    categories,
                    locations,
                    clock,
                    zoneId,
                ) as T
            }
        }
    }
}
