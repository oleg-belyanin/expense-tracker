package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.data.filters.ExpenseListFilterStore
import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.LocationRepository
import com.olegbelyanin.expensetracker.domain.PersistExpenseRequest
import com.olegbelyanin.expensetracker.domain.expense.DeleteExpenseUseCase
import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.domain.expense.ExpenseSearchQuery
import com.olegbelyanin.expensetracker.domain.expense.ObserveExpenseListUseCase
import com.olegbelyanin.expensetracker.domain.expense.SuggestLocationsUseCase
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.ui.expenses.ExpensesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-09-04T08:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun restoresPersistedFilterAndSavesApply() = runTest(dispatcher) {
        val store = MemoryFilterStore(
            ExpenseListFilter(preset = ExpensePeriodPreset.CURRENT_MONTH, categoryIds = setOf(2)),
        )
        val viewModel = viewModel(store)
        runCurrent()

        assertEquals(ExpensePeriodPreset.CURRENT_MONTH, viewModel.periodPreset.value)
        assertEquals(setOf(2L), viewModel.selectedCategoryIds.value)

        viewModel.onResetFilters()
        runCurrent()
        assertEquals(ExpensePeriodPreset.ALL, store.value.preset)
        assertEquals(emptySet<Long>(), store.value.categoryIds)
    }

    @Test
    fun applyFromAnalyticsPersistsSlice() = runTest(dispatcher) {
        val store = MemoryFilterStore()
        val viewModel = viewModel(store)
        runCurrent()

        viewModel.applyFromAnalytics(
            ExpenseListFilter(preset = ExpensePeriodPreset.YEAR, categoryIds = setOf(4)),
        )
        runCurrent()

        assertEquals(ExpensePeriodPreset.YEAR, store.value.preset)
        assertEquals(setOf(4L), store.value.categoryIds)
    }

    private fun viewModel(store: ExpenseListFilterStore): ExpensesViewModel {
        val categories = FakeCategories()
        val locations = FakeLocations()
        val expenses = FakeExpenses()
        return ExpensesViewModel(
            observeList = ObserveExpenseListUseCase(expenses, categories, locations, clock, ZoneOffset.UTC),
            deleteExpense = DeleteExpenseUseCase(expenses),
            suggestLocations = SuggestLocationsUseCase(locations),
            categories = categories,
            locations = locations,
            filterStore = store,
            clock = clock,
            zoneId = ZoneOffset.UTC,
        )
    }

    private class MemoryFilterStore(initial: ExpenseListFilter = ExpenseListFilter()) : ExpenseListFilterStore {
        var value: ExpenseListFilter = initial

        override suspend fun load(): ExpenseListFilter = value

        override suspend fun save(filter: ExpenseListFilter) {
            value = filter.copy(query = "")
        }
    }

    private class FakeCategories : CategoryRepository {
        override suspend fun getActiveCategories() = emptyList<Category>()

        override fun observeActiveCategories() = MutableStateFlow(emptyList<Category>())

        override fun observeArchivedCategories() = MutableStateFlow(emptyList<Category>())

        override fun observeAll() = MutableStateFlow(emptyList<Category>())

        override suspend fun findById(id: Long) = null

        override suspend fun requireFallback() = error("unused")

        override suspend fun createUserCategory(name: String, color: String, icon: String) = error("unused")

        override suspend fun updateUserCategory(id: Long, name: String, color: String, icon: String) = error("unused")

        override suspend fun archive(id: Long) = error("unused")

        override suspend fun restore(id: Long) = error("unused")
    }

    private class FakeLocations : LocationRepository {
        override suspend fun suggest(query: String, limit: Int) = emptyList<Location>()

        override suspend fun findById(id: Long) = null

        override fun observeAll() = MutableStateFlow(emptyList<Location>())
    }

    private class FakeExpenses : ExpenseRepository {
        override suspend fun get(id: String) = null

        override suspend fun getAll() = emptyList<Expense>()

        override suspend fun findByDedupKey(dedupKey: String) = null

        override fun observeAll() = MutableStateFlow(emptyList<Expense>())

        override fun observeCount() = MutableStateFlow(0)

        override fun observeMatching(query: ExpenseSearchQuery): Flow<List<Expense>> = MutableStateFlow(emptyList())

        override suspend fun persist(request: PersistExpenseRequest) = error("unused")

        override suspend fun delete(id: String) = Unit

        override suspend fun clearHistory() = 0
    }
}
