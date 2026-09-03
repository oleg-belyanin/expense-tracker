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
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.model.Period
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

data class SavedExpenseToast(val expenseId: String)

enum class ExpensesDialog {
    None,
    Categories,
    Locations,
    Periods,
    CustomStart,
    CustomEnd,
}

class ExpensesViewModel(
    private val observeList: ObserveExpenseListUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
    categories: CategoryRepository,
    locations: LocationRepository,
    clock: Clock,
    zoneId: ZoneId,
) : ViewModel() {
    val today: LocalDate = LocalDate.now(clock.withZone(zoneId))

    private val query = MutableStateFlow("")
    private val preset = MutableStateFlow(ExpensePeriodPreset.ALL)
    private val customPeriod = MutableStateFlow<Period?>(null)
    private val categoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val locationId = MutableStateFlow<Long?>(null)
    private val _toast = MutableStateFlow<SavedExpenseToast?>(null)
    private val dialog = MutableStateFlow(ExpensesDialog.None)
    private val draftPreset = MutableStateFlow(ExpensePeriodPreset.ALL)
    private val draftCustom = MutableStateFlow<Period?>(null)
    private var toastJob: Job? = null

    val queryText: StateFlow<String> = query.asStateFlow()
    val periodPreset: StateFlow<ExpensePeriodPreset> = preset.asStateFlow()
    val customPeriodRange: StateFlow<Period?> = customPeriod.asStateFlow()
    val selectedCategoryIds: StateFlow<Set<Long>> = categoryIds.asStateFlow()
    val selectedLocationId: StateFlow<Long?> = locationId.asStateFlow()
    val toast: StateFlow<SavedExpenseToast?> = _toast.asStateFlow()
    val dialogState: StateFlow<ExpensesDialog> = dialog.asStateFlow()
    val draftPresetState: StateFlow<ExpensePeriodPreset> = draftPreset.asStateFlow()
    val draftCustomState: StateFlow<Period?> = draftCustom.asStateFlow()

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val slice: StateFlow<ExpenseListSlice?> =
        combine(query, preset, customPeriod, categoryIds, locationId) { text, period, custom, ids, place ->
            ExpenseListFilter(
                query = text,
                preset = period,
                customPeriod = custom,
                categoryIds = ids,
                locationId = place,
            )
        }.flatMapLatest { observeList.observe(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    fun onToggleCategory(id: Long) {
        val next = categoryIds.value.toMutableSet()
        if (!next.add(id)) {
            next.remove(id)
        }
        categoryIds.value = next
    }

    fun onSelectLocation(id: Long) {
        locationId.value = id.takeIf { it != locationId.value }
    }

    fun onOpenCategoryFilter() {
        dialog.value = ExpensesDialog.Categories
    }

    fun onOpenLocationFilter() {
        dialog.value = ExpensesDialog.Locations
    }

    fun onOpenPeriodFilter() {
        draftPreset.value = preset.value
        draftCustom.value = customPeriod.value ?: ExpensePeriodResolver.defaultCustom(today)
        dialog.value = ExpensesDialog.Periods
    }

    fun onDismissDialog() {
        dialog.value = ExpensesDialog.None
    }

    fun onDraftPreset(value: ExpensePeriodPreset) {
        draftPreset.value = value
        if (value == ExpensePeriodPreset.CUSTOM) {
            if (draftCustom.value == null) {
                draftCustom.value = ExpensePeriodResolver.defaultCustom(today)
            }
            dialog.value = ExpensesDialog.CustomStart
        }
    }

    fun onCustomStart(date: LocalDate) {
        val end = draftCustom.value?.endInclusive ?: today
        draftCustom.value = Period.of(date, end)
        dialog.value = ExpensesDialog.CustomEnd
    }

    fun onCustomEnd(date: LocalDate) {
        val start = draftCustom.value?.startInclusive ?: today
        draftCustom.value = Period.of(start, date)
        dialog.value = ExpensesDialog.Periods
    }

    fun onApplyPeriod() {
        val next = draftPreset.value
        preset.value = next
        customPeriod.value = if (next == ExpensePeriodPreset.CUSTOM) draftCustom.value else null
        dialog.value = ExpensesDialog.None
    }

    fun onResetFilters() {
        query.value = ""
        preset.value = ExpensePeriodPreset.ALL
        customPeriod.value = null
        categoryIds.value = emptySet()
        locationId.value = null
    }

    fun onExpenseSaved(expenseId: String) {
        _toast.value = SavedExpenseToast(expenseId)
        toastJob?.cancel()
        toastJob =
            viewModelScope.launch {
                delay(4_000)
                if (_toast.value?.expenseId == expenseId) {
                    _toast.value = null
                }
            }
    }

    fun onUndoSaved() {
        val id = _toast.value?.expenseId ?: return
        toastJob?.cancel()
        _toast.value = null
        viewModelScope.launch { deleteExpense(id) }
    }

    companion object {
        fun factory(
            observeList: ObserveExpenseListUseCase,
            deleteExpense: DeleteExpenseUseCase,
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
                    categories,
                    locations,
                    clock,
                    zoneId,
                ) as T
            }
        }
    }
}
