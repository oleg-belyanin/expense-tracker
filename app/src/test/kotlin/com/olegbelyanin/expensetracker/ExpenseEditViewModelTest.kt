package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.LocationRepository
import com.olegbelyanin.expensetracker.domain.PersistExpenseRequest
import com.olegbelyanin.expensetracker.domain.expense.ExpenseInputValidator
import com.olegbelyanin.expensetracker.domain.expense.ExpenseSearchQuery
import com.olegbelyanin.expensetracker.domain.expense.SaveExpenseCommand
import com.olegbelyanin.expensetracker.domain.expense.SaveExpenseResult
import com.olegbelyanin.expensetracker.model.CategorizationResult
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.ExpenseNameSuggestion
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.model.Money
import com.olegbelyanin.expensetracker.ui.components.KeypadKey
import com.olegbelyanin.expensetracker.ui.expense.ExpenseEditNotice
import com.olegbelyanin.expensetracker.ui.expense.ExpenseEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseEditViewModelTest {
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
    fun saveFailureShowsHumanNoticeAndUnlocksButton() = runTest(dispatcher) {
        val saved = mutableListOf<String>()
        val viewModel =
            viewModel(
                saveExpense = { error("disk full SQLITE_IOERR") },
            )
        runCurrent()

        viewModel.onAmountKey(KeypadKey.Digit('1'))
        viewModel.onAmountKey(KeypadKey.Digit('5'))
        viewModel.onAmountKey(KeypadKey.Digit('0'))
        viewModel.onNameChange("Латте")
        viewModel.onSave { saved += it }
        runCurrent()

        assertTrue(saved.isEmpty())
        assertFalse(viewModel.state.value.saving)
        assertEquals(ExpenseEditNotice.SaveFailed, viewModel.state.value.notice)
        assertTrue(viewModel.canSave())
    }

    @Test
    fun deleteFailureShowsHumanNoticeAndKeepsExpense() = runTest(dispatcher) {
        var deleted = false
        val viewModel =
            viewModel(
                expenseId = EXISTING.id,
                expenses = FakeExpenses(EXISTING),
                deleteExpense = { error("constraint") },
            )
        runCurrent()

        viewModel.onConfirmDelete { deleted = true }
        runCurrent()

        assertFalse(deleted)
        assertFalse(viewModel.state.value.deleting)
        assertEquals(ExpenseEditNotice.DeleteFailed, viewModel.state.value.notice)
    }

    @Test
    fun namePrefixShowsSuggestionsAndSelectionFillsName() = runTest(dispatcher) {
        val ketosteril = ExpenseNameSuggestion(
            name = "Кетостерил",
            normalizedName = "кетостер",
            usageCount = 2,
            lastUsedAt = Instant.parse("2026-09-04T00:00:00Z"),
        )
        val ketchup = ExpenseNameSuggestion(
            name = "Кетчуп",
            normalizedName = "кетчуп",
            usageCount = 1,
            lastUsedAt = Instant.parse("2026-07-25T00:00:00Z"),
        )
        val viewModel =
            viewModel(
                suggestNames = { query ->
                    when {
                        query.startsWith("Кетост") -> listOf(ketosteril)
                        query.startsWith("Кет") -> listOf(ketosteril, ketchup)
                        else -> emptyList()
                    }
                },
                suggestCategory = { name, _ ->
                    if (name == "Кетостерил") HEALTH_RESULT else FALLBACK
                },
            )
        runCurrent()

        viewModel.onNameChange("Кет")
        advanceTimeBy(150)
        runCurrent()

        assertEquals(listOf("Кетостерил", "Кетчуп"), viewModel.state.value.nameSuggestions.map { it.name })
        assertEquals(OTHER.id, viewModel.state.value.category?.id)

        viewModel.onNameChange("Кетост")
        advanceTimeBy(150)
        runCurrent()

        assertEquals(listOf("Кетостерил"), viewModel.state.value.nameSuggestions.map { it.name })
        assertEquals(HEALTH.id, viewModel.state.value.category?.id)

        viewModel.onNameSuggestion(ketosteril)
        advanceTimeBy(150)
        runCurrent()

        assertEquals("Кетостерил", viewModel.state.value.name)
        assertTrue(viewModel.state.value.nameSuggestions.isEmpty())
        assertEquals(HEALTH.id, viewModel.state.value.category?.id)
    }

    @Test
    fun emptyFocusKeepsThreeRecentSuggestions() = runTest(dispatcher) {
        val names = (1..8).map { index ->
            ExpenseNameSuggestion(
                name = "Название $index",
                normalizedName = "название $index",
                usageCount = 1,
                lastUsedAt = Instant.parse("2026-09-04T00:00:00Z"),
            )
        }
        val places = (1..8).map { index ->
            Location(
                id = index.toLong(),
                name = "Место $index",
                normalizedName = "место $index",
                usageCount = 1,
                lastUsedAt = Instant.parse("2026-09-04T00:00:00Z"),
                archivedAt = null,
            )
        }
        val viewModel =
            viewModel(
                suggestNames = { query -> if (query.isEmpty()) names else emptyList() },
                suggestLocations = { query -> if (query.isEmpty()) places else emptyList() },
            )
        runCurrent()

        viewModel.onNameFocus(true)
        viewModel.onLocationFocus(true)
        advanceTimeBy(150)
        runCurrent()

        assertEquals(
            listOf("Название 1", "Название 2", "Название 3"),
            viewModel.state.value.nameSuggestions.map { it.name },
        )
        assertEquals(
            listOf("Место 1", "Место 2", "Место 3"),
            viewModel.state.value.locationSuggestions.map { it.name },
        )
    }

    private fun viewModel(
        expenseId: String? = null,
        expenses: ExpenseRepository = FakeExpenses(),
        saveExpense: suspend (SaveExpenseCommand) -> SaveExpenseResult = { error("unused") },
        deleteExpense: suspend (String) -> Unit = {},
        suggestNames: suspend (String) -> List<ExpenseNameSuggestion> = { emptyList() },
        suggestLocations: suspend (String) -> List<Location> = { emptyList() },
        suggestCategory: suspend (String, String?) -> CategorizationResult = { _, _ -> FALLBACK },
    ) = ExpenseEditViewModel(
        expenseId = expenseId,
        expenses = expenses,
        categories = FakeCategories(OTHER, HEALTH),
        locations = FakeLocations(),
        saveExpense = saveExpense,
        deleteExpense = deleteExpense,
        suggestLocations = suggestLocations,
        suggestNames = suggestNames,
        suggestCategory = suggestCategory,
        createCategory = { error("unused") },
        validator = ExpenseInputValidator(),
        clock = clock,
        zoneId = ZoneOffset.UTC,
    )

    private class FakeCategories(private vararg val categories: Category) : CategoryRepository {
        override suspend fun getActiveCategories() = categories.toList()

        override fun observeActiveCategories() = MutableStateFlow(categories.toList())

        override fun observeArchivedCategories() = MutableStateFlow(emptyList<Category>())

        override fun observeAll() = MutableStateFlow(categories.toList())

        override suspend fun findById(id: Long) = categories.find { it.id == id }

        override suspend fun requireFallback() = categories.first()

        override suspend fun createUserCategory(name: String, color: String, icon: String) = error("unused")

        override suspend fun updateUserCategory(id: Long, name: String, color: String, icon: String) = error("unused")

        override suspend fun archive(id: Long) = error("unused")

        override suspend fun restore(id: Long) = error("unused")
    }

    private class FakeExpenses(private val expense: Expense? = null) : ExpenseRepository {
        override suspend fun get(id: String) = expense?.takeIf { it.id == id }

        override suspend fun getAll() = listOfNotNull(expense)

        override suspend fun findByDedupKey(dedupKey: String) = null

        override fun observeAll() = MutableStateFlow(listOfNotNull(expense))

        override fun observeMatching(query: ExpenseSearchQuery): Flow<List<Expense>> = observeAll()

        override suspend fun persist(request: PersistExpenseRequest) = error("unused")

        override suspend fun delete(id: String) = error("unused")

        override suspend fun clearHistory() = 0
    }

    private class FakeLocations : LocationRepository {
        override suspend fun suggest(query: String, limit: Int) = emptyList<Location>()

        override suspend fun findById(id: Long) = null

        override fun observeAll() = MutableStateFlow(emptyList<Location>())
    }

    companion object {
        private val OTHER =
            Category(
                id = 1,
                code = "OTHER",
                name = "Прочее",
                normalizedName = "прочее",
                color = "#546E7A",
                icon = "other",
                isBuiltin = true,
                archivedAt = null,
            )
        private val EXISTING =
            Expense(
                id = "e1",
                amount = Money(15000),
                spentAt = Instant.parse("2026-09-04T00:00:00Z"),
                name = "Латте",
                normalizedName = "латте",
                categoryId = 1,
                locationId = null,
                comment = null,
                categoryAssignmentSource = CategoryAssignmentSource.FALLBACK,
                dedupKey = "user:e1",
            )
        private val HEALTH =
            Category(
                id = 4,
                code = "HEALTH",
                name = "Здоровье",
                normalizedName = "здоровье",
                color = "#E57373",
                icon = "health",
                isBuiltin = true,
                archivedAt = null,
            )
        private val FALLBACK =
            CategorizationResult(
                selectedCategoryId = 1,
                orderedCandidates = emptyList(),
                source = CategoryAssignmentSource.FALLBACK,
                confidence = 0.0,
                matchedFeatures = emptyList(),
                usedFallback = true,
            )
        private val HEALTH_RESULT =
            CategorizationResult(
                selectedCategoryId = 4,
                orderedCandidates = emptyList(),
                source = CategoryAssignmentSource.PROBABILISTIC,
                confidence = 0.9,
                matchedFeatures = emptyList(),
                usedFallback = false,
            )
    }
}
