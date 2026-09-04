package com.olegbelyanin.expensetracker.domain.backup

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.LocationRepository
import com.olegbelyanin.expensetracker.domain.PersistExpenseRequest
import com.olegbelyanin.expensetracker.model.Category
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

class BackupUseCaseTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-04T08:00:00Z"), ZoneOffset.UTC)

    @Test
    fun exportCsvIncludesCategoryForEachExpense() = runTest {
        val csv = ExportExpensesCsvUseCase(
            expenses = FakeExpenses(storedExpense()),
            categories = FakeCategories(storedCategory()),
            locations = FakeLocations(storedLocation()),
        )()
        val row = ExpenseCsv.parse(csv).single()
        assertEquals("Латте", row.name)
        assertEquals("Кафе", row.categoryName)
        assertEquals("CAFE", row.categoryCode)
        assertEquals("Шоколадница", row.locationName)
        assertEquals(15_050, row.amountMinor)
    }

    @Test
    fun restoreTwiceDoesNotDuplicateHistoryOrExamples() = runTest {
        val source = InMemoryBackupRepository(sampleBackupSnapshot())
        val json = CreateBackupUseCase(source, clock)()
        val target = InMemoryBackupRepository(
            sampleBackupSnapshot(includeExpense = false, includeLearning = false),
        )
        val restore = RestoreBackupUseCase(target)
        val first = restore(json)
        val second = restore(json)
        assertEquals(1, first.expensesInserted)
        assertEquals(0, first.expensesSkipped)
        assertEquals(1, first.examplesInserted)
        assertEquals(0, second.expensesInserted)
        assertEquals(1, second.expensesSkipped)
        assertEquals(0, second.examplesInserted)
        assertEquals(1, second.examplesSkipped)
        assertEquals(1, target.snapshot.expenses.size)
        assertEquals(1, target.snapshot.learningExamples.size)
        assertEquals(1, target.snapshot.exactRules.size)
    }

    @Test
    fun corruptedRestoreDoesNotTouchExistingData() = runTest {
        val target = InMemoryBackupRepository(sampleBackupSnapshot())
        val before = target.snapshot
        try {
            RestoreBackupUseCase(target)("""{"not":"a-backup"}""")
            throw AssertionError("expected BackupCorruptedException")
        } catch (error: BackupCorruptedException) {
            assertTrue(error.userMessage.contains("не изменены"))
        }
        assertTrue(target.applyCalls.isEmpty())
        assertEquals(before, target.snapshot)
    }
}

class ClearExpenseHistoryUseCaseContractTest {
    @Test
    fun clearHistoryRemovesExpensesAndDetachesExamplesButKeepsRules() = runTest {
        val store = LearningAwareExpenses()
        val deleted = com.olegbelyanin.expensetracker.domain.expense.ClearExpenseHistoryUseCase(store)()
        assertEquals(1, deleted)
        assertTrue(store.getAll().isEmpty())
        assertTrue(store.examples.all { it.expenseId == null })
        assertEquals(listOf("латте"), store.exactRules)
        assertEquals(listOf("Стоматология"), store.categories)
    }
}

private fun storedExpense() = Expense(
    id = "e-1",
    amount = Money(15_050),
    spentAt = Instant.parse("2026-09-03T00:00:00Z"),
    name = "Латте",
    normalizedName = "латте",
    categoryId = 2,
    locationId = 7,
    comment = "утром",
    categoryAssignmentSource = CategoryAssignmentSource.EXPLICIT,
    dedupKey = "user:e-1",
)

private fun storedCategory() = Category(
    id = 2,
    code = "CAFE",
    name = "Кафе",
    normalizedName = "кафе",
    color = "#E65100",
    icon = "cafe",
    isBuiltin = true,
    archivedAt = null,
)

private fun storedLocation() = Location(
    id = 7,
    name = "Шоколадница",
    normalizedName = "шоколадниц",
    usageCount = 1,
    lastUsedAt = Instant.parse("2026-09-03T00:00:00Z"),
    archivedAt = null,
)

private class FakeExpenses(vararg initial: Expense) : ExpenseRepository {
    private val stored = initial.associateBy { it.id }.toMutableMap()

    override suspend fun get(id: String): Expense? = stored[id]

    override suspend fun getAll(): List<Expense> = stored.values.toList()

    override suspend fun findByDedupKey(dedupKey: String): Expense? = stored.values.find { it.dedupKey == dedupKey }

    override fun observeAll(): Flow<List<Expense>> = MutableStateFlow(stored.values.toList())

    override suspend fun persist(request: PersistExpenseRequest): Expense = error("not used")

    override suspend fun delete(id: String) {
        stored.remove(id)
    }

    override suspend fun clearHistory(): Int {
        val deleted = stored.size
        stored.clear()
        return deleted
    }
}

private class FakeCategories(vararg initial: Category) : CategoryRepository {
    private val stored = initial.toList()

    override suspend fun getActiveCategories(): List<Category> = stored.filter { it.isActive }

    override fun observeActiveCategories(): Flow<List<Category>> = MutableStateFlow(stored.filter { it.isActive })

    override fun observeArchivedCategories(): Flow<List<Category>> = MutableStateFlow(stored.filter { !it.isActive })

    override fun observeAll(): Flow<List<Category>> = MutableStateFlow(stored)

    override suspend fun findById(id: Long): Category? = stored.find { it.id == id }

    override suspend fun requireFallback(): Category = stored.first()

    override suspend fun createUserCategory(name: String, color: String, icon: String): Category = error("not used")

    override suspend fun updateUserCategory(id: Long, name: String, color: String, icon: String): Category =
        error("not used")

    override suspend fun archive(id: Long): Category = error("not used")

    override suspend fun restore(id: Long): Category = error("not used")
}

private class FakeLocations(vararg initial: Location) : LocationRepository {
    private val stored = initial.toList()

    override suspend fun suggest(query: String, limit: Int): List<Location> = stored.take(limit)

    override suspend fun findById(id: Long): Location? = stored.find { it.id == id }

    override fun observeAll(): Flow<List<Location>> = MutableStateFlow(stored)
}

private data class DetachedExample(var expenseId: String?)

private class LearningAwareExpenses : ExpenseRepository {
    private val stored = mutableMapOf("e-1" to storedExpense())
    val examples = mutableListOf(DetachedExample("e-1"))
    val exactRules = listOf("латте")
    val categories = listOf("Стоматология")

    override suspend fun get(id: String): Expense? = stored[id]

    override suspend fun getAll(): List<Expense> = stored.values.toList()

    override suspend fun findByDedupKey(dedupKey: String): Expense? = stored.values.find { it.dedupKey == dedupKey }

    override fun observeAll(): Flow<List<Expense>> = MutableStateFlow(stored.values.toList())

    override suspend fun persist(request: PersistExpenseRequest): Expense = error("not used")

    override suspend fun delete(id: String) {
        stored.remove(id)
    }

    override suspend fun clearHistory(): Int {
        val deleted = stored.size
        stored.clear()
        examples.forEach { it.expenseId = null }
        return deleted
    }
}
