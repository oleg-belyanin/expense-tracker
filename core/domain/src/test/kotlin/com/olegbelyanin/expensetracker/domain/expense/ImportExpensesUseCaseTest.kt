package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.categorization.CategorizationCatalog
import com.olegbelyanin.expensetracker.categorization.CategorizationLookup
import com.olegbelyanin.expensetracker.categorization.CategorizationQuery
import com.olegbelyanin.expensetracker.categorization.CategorizationSnapshot
import com.olegbelyanin.expensetracker.categorization.CategoryVector
import com.olegbelyanin.expensetracker.categorization.ExactMatch
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.LocationRepository
import com.olegbelyanin.expensetracker.domain.PersistExpenseRequest
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ImportExpensesUseCaseTest {
    private val zone = ZoneOffset.UTC
    private val clock = Clock.fixed(Instant.parse("2026-09-03T10:15:00Z"), zone)
    private val spentAt = Instant.parse("2026-09-01T00:00:00Z")

    @Test
    fun autoCategorizesWithoutInteractiveLearning() = runTest {
        val expenses = RecordingExpenseRepository()
        val result = useCase(expenses)(listOf(draft()))
        val request = expenses.persistCalls.single()
        assertEquals(1, result.imported.size)
        assertEquals(0, result.skippedDuplicate)
        assertEquals(0, result.skippedInvalid)
        assertEquals(false, request.interactive)
        assertEquals(2L, request.categoryId)
        assertEquals(CategoryAssignmentSource.PROBABILISTIC, request.categoryAssignmentSource)
        assertEquals(2L, request.proposedCategoryId)
        assertEquals("Латте", request.name)
        assertEquals(
            ImportExpensesUseCase.contentDedupKey("Латте", spentAt, Money(15_000), "Шоколадница"),
            request.dedupKey,
        )
    }

    @Test
    fun skipsDuplicateByContentKeyAndById() = runTest {
        val expenses = RecordingExpenseRepository()
        val useCase = useCase(expenses)
        val first = useCase(listOf(draft()))
        val sameContent = useCase(listOf(draft(id = "other")))
        val sameId = useCase(listOf(draft(id = first.imported.single().id, name = "Капучино")))
        assertEquals(1, first.imported.size)
        assertEquals(0, sameContent.imported.size)
        assertEquals(1, sameContent.skippedDuplicate)
        assertEquals(0, sameId.imported.size)
        assertEquals(1, sameId.skippedDuplicate)
        assertEquals(1, expenses.persistCalls.size)
    }

    @Test
    fun skipsEmptyNameAndFutureDate() = runTest {
        val expenses = RecordingExpenseRepository()
        val result = useCase(expenses)(
            listOf(
                draft(name = "   "),
                draft(name = "Завтра", spentAt = Instant.parse("2026-09-04T00:00:00Z")),
            ),
        )
        assertTrue(result.imported.isEmpty())
        assertEquals(2, result.skippedInvalid)
        assertTrue(expenses.persistCalls.isEmpty())
    }

    private fun useCase(expenses: RecordingExpenseRepository) = ImportExpensesUseCase(
        expenses = expenses,
        suggestCategory = SuggestCategoryUseCase(seedCatalog()),
        clock = clock,
        zoneId = zone,
    )

    private fun draft(
        id: String? = null,
        name: String = "Латте",
        spentAt: Instant = this.spentAt,
    ) = ImportExpenseDraft(
        id = id,
        amount = Money(15_000),
        spentAt = spentAt,
        name = name,
        locationName = "Шоколадница",
    )
}

class RecalculateCategoriesUseCaseTest {
    @Test
    fun updatesUnlockedExpenseWithoutInteractiveLearning() = runTest {
        val existing = storedExpense(categoryId = 10, source = CategoryAssignmentSource.FALLBACK)
        val expenses = RecordingExpenseRepository(existing)
        val result = RecalculateCategoriesUseCase(
            expenses = expenses,
            locations = FakeLocationRepository(),
            suggestCategory = SuggestCategoryUseCase(seedCatalog()),
        )()
        val request = expenses.persistCalls.single()
        assertEquals(1, result.updated)
        assertEquals(0, result.unchanged)
        assertEquals(0, result.skippedExplicit)
        assertEquals(false, request.interactive)
        assertEquals(2L, request.categoryId)
        assertEquals(CategoryAssignmentSource.PROBABILISTIC, request.categoryAssignmentSource)
        assertEquals(existing.dedupKey, request.dedupKey)
    }

    @Test
    fun skipsExplicitChoiceAndLeavesMatchingExpense() = runTest {
        val locked = storedExpense(id = "e-lock", source = CategoryAssignmentSource.EXPLICIT)
        val matching = storedExpense(id = "e-ok", source = CategoryAssignmentSource.PROBABILISTIC)
        val expenses = RecordingExpenseRepository(locked, matching)
        val result = RecalculateCategoriesUseCase(
            expenses = expenses,
            locations = FakeLocationRepository(),
            suggestCategory = SuggestCategoryUseCase(seedCatalog()),
        )()
        assertEquals(0, result.updated)
        assertEquals(1, result.unchanged)
        assertEquals(1, result.skippedExplicit)
        assertTrue(expenses.persistCalls.isEmpty())
        assertEquals(CategoryAssignmentSource.EXPLICIT, expenses.get("e-lock")?.categoryAssignmentSource)
    }
}

private fun seedCatalog(categoryId: Long = 2, fallbackId: Long = 10) = CategorizationCatalog { name, _ ->
    CategorizationLookup(
        query = CategorizationQuery(name.lowercase(), emptyList()),
        snapshot = CategorizationSnapshot(
            fallbackCategoryId = fallbackId,
            activeCategoryIds = setOf(categoryId, fallbackId),
            seedExact = ExactMatch(categoryId, CategoryVector.SOURCE_SEED),
        ),
    )
}

private fun storedExpense(
    id: String = "e1",
    categoryId: Long = 2,
    source: CategoryAssignmentSource = CategoryAssignmentSource.PROBABILISTIC,
) = Expense(
    id = id,
    amount = Money(15_000),
    spentAt = Instant.parse("2026-09-01T00:00:00Z"),
    name = "Латте",
    normalizedName = "латте",
    categoryId = categoryId,
    locationId = null,
    comment = null,
    categoryAssignmentSource = source,
    dedupKey = "user:$id",
)

private class RecordingExpenseRepository(vararg initial: Expense) : ExpenseRepository {
    private val stored = initial.associateBy { it.id }.toMutableMap()
    val persistCalls = mutableListOf<PersistExpenseRequest>()

    override suspend fun get(id: String): Expense? = stored[id]

    override suspend fun getAll(): List<Expense> = stored.values.toList()

    override suspend fun findByDedupKey(dedupKey: String): Expense? = stored.values.find { it.dedupKey == dedupKey }

    override fun observeAll(): Flow<List<Expense>> = MutableStateFlow(stored.values.toList())

    override suspend fun persist(request: PersistExpenseRequest): Expense {
        persistCalls += request
        val expense = Expense(
            id = request.id,
            amount = request.amount,
            spentAt = request.spentAt,
            name = request.name,
            normalizedName = request.name.lowercase(),
            categoryId = request.categoryId,
            locationId = null,
            comment = request.comment,
            categoryAssignmentSource = request.categoryAssignmentSource,
            dedupKey = request.dedupKey ?: "user:${request.id}",
        )
        stored[expense.id] = expense
        return expense
    }

    override suspend fun delete(id: String) {
        stored.remove(id)
    }

    override suspend fun clearHistory(): Int {
        val deleted = stored.size
        stored.clear()
        return deleted
    }
}

private class FakeLocationRepository : LocationRepository {
    override suspend fun suggest(query: String, limit: Int) = emptyList<Location>()

    override suspend fun findById(id: Long) = null

    override fun observeAll() = MutableStateFlow(emptyList<Location>())
}
