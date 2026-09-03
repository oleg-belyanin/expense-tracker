package com.olegbelyanin.expensetracker.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.expense.DeleteExpenseUseCase
import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.domain.expense.ExpenseListSlice
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.domain.expense.ObserveExpenseListUseCase
import com.olegbelyanin.expensetracker.model.Category
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

data class SavedExpenseToast(val expenseId: String)

class ExpensesViewModel(
    private val observeList: ObserveExpenseListUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
    categories: CategoryRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val preset = MutableStateFlow(ExpensePeriodPreset.ALL)
    private val categoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _toast = MutableStateFlow<SavedExpenseToast?>(null)
    private val _categoryFilterOpen = MutableStateFlow(false)
    private var toastJob: Job? = null

    val queryText: StateFlow<String> = query.asStateFlow()
    val periodPreset: StateFlow<ExpensePeriodPreset> = preset.asStateFlow()
    val selectedCategoryIds: StateFlow<Set<Long>> = categoryIds.asStateFlow()
    val toast: StateFlow<SavedExpenseToast?> = _toast.asStateFlow()
    val categoryFilterOpen: StateFlow<Boolean> = _categoryFilterOpen.asStateFlow()

    val activeCategories: StateFlow<List<Category>> =
        categories.observeActiveCategories().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val slice: StateFlow<ExpenseListSlice?> =
        combine(query, preset, categoryIds) { text, period, ids ->
            ExpenseListFilter(query = text, preset = period, categoryIds = ids)
        }.flatMapLatest { observeList.observe(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onPeriodPreset(value: ExpensePeriodPreset) {
        preset.value = value
    }

    fun onToggleCategory(id: Long) {
        val next = categoryIds.value.toMutableSet()
        if (!next.add(id)) {
            next.remove(id)
        }
        categoryIds.value = next
    }

    fun onOpenCategoryFilter() {
        _categoryFilterOpen.value = true
    }

    fun onDismissCategoryFilter() {
        _categoryFilterOpen.value = false
    }

    fun onResetFilters() {
        query.value = ""
        preset.value = ExpensePeriodPreset.ALL
        categoryIds.value = emptySet()
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
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ExpensesViewModel(observeList, deleteExpense, categories) as T
            }
        }
    }
}
