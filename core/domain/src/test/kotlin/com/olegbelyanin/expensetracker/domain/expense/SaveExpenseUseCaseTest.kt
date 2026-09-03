package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.PersistExpenseRequest
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SaveExpenseUseCaseTest {
    private val zone = ZoneOffset.UTC
    private val clock = Clock.fixed(Instant.parse("2026-09-03T10:15:00Z"), zone)
    private val today = LocalDate.of(2026, 9, 3)
    private val cafe = category(id = 2)

    @Test
    fun persistsValidExpenseWithDefaultToday() = runTest {
        val expenses = FakeExpenseRepository()
        val useCase = useCase(expenses)
        val result = useCase(
            SaveExpenseCommand(
                amountInput = "150,50",
                name = "  Латте  ",
                categoryId = cafe.id,
                locationName = " Шоколадница ",
                comment = "  ",
                categoryAssignmentSource = CategoryAssignmentSource.PROBABILISTIC,
            ),
        )
        val success = result as SaveExpenseResult.Success
        assertEquals(1, expenses.persistCalls.size)
        val request = expenses.persistCalls.single()
        assertEquals(15_050, request.amount.minor)
        assertEquals(today.atStartOfDay(zone).toInstant(), request.spentAt)
        assertEquals("Латте", request.name)
        assertEquals("Шоколадница", request.locationName)
        assertEquals(null, request.comment)
        assertEquals(success.expense.id, request.id)
    }

    @Test
    fun blocksInvalidAmountAndKeepsNoPersist() = runTest {
        val expenses = FakeExpenseRepository()
        val useCase = useCase(expenses)
        val empty = useCase(command(amount = "")) as SaveExpenseResult.Invalid
        val zero = useCase(command(amount = "0")) as SaveExpenseResult.Invalid
        val negative = useCase(command(amount = "-1")) as SaveExpenseResult.Invalid
        val letters = useCase(command(amount = "сто")) as SaveExpenseResult.Invalid
        assertEquals(AmountFieldError.EMPTY, empty.errors.amount)
        assertEquals(AmountFieldError.ZERO, zero.errors.amount)
        assertEquals(AmountFieldError.NEGATIVE, negative.errors.amount)
        assertEquals(AmountFieldError.NON_NUMERIC, letters.errors.amount)
        assertTrue(expenses.persistCalls.isEmpty())
    }

    @Test
    fun rejectsFutureDateTheSameWayOnCreateAndEdit() = runTest {
        val expenses = FakeExpenseRepository()
        val useCase = useCase(expenses)
        val create = useCase(command(spentAt = today.plusDays(1))) as SaveExpenseResult.Invalid
        val edit = useCase(command(id = "e1", spentAt = today.plusDays(3))) as SaveExpenseResult.Invalid
        assertEquals(DateFieldError.FUTURE, create.errors.date)
        assertEquals(DateFieldError.FUTURE, edit.errors.date)
        assertTrue(expenses.persistCalls.isEmpty())
    }

    @Test
    fun rejectsMissingAndArchivedCategoryForNewExpense() = runTest {
        val expenses = FakeExpenseRepository()
        val archived = category(id = 9, archived = true)
        val useCase = useCase(expenses, listOf(cafe, archived))
        val missing = useCase(command(categoryId = 404)) as SaveExpenseResult.Invalid
        val blocked = useCase(command(categoryId = archived.id)) as SaveExpenseResult.Invalid
        assertEquals(CategoryFieldError.MISSING, missing.errors.category)
        assertEquals(CategoryFieldError.ARCHIVED, blocked.errors.category)
    }

    @Test
    fun allowsKeepingArchivedCategoryOnExistingExpense() = runTest {
        val archived = category(id = 9, archived = true)
        val existing = expense(id = "e1", categoryId = archived.id)
        val expenses = FakeExpenseRepository(existing)
        val useCase = useCase(expenses, listOf(cafe, archived))
        val result = useCase(command(id = "e1", categoryId = archived.id))
        assertTrue(result is SaveExpenseResult.Success)
        assertEquals(1, expenses.persistCalls.size)
    }

    @Test
    fun deleteRemovesStoredExpense() = runTest {
        val existing = expense(id = "e1", categoryId = cafe.id)
        val expenses = FakeExpenseRepository(existing)
        DeleteExpenseUseCase(expenses)("e1")
        assertEquals(null, expenses.get("e1"))
    }

    private fun useCase(expenses: FakeExpenseRepository, categories: List<Category> = listOf(cafe)) =
        SaveExpenseUseCase(
            expenses = expenses,
            categories = FakeCategoryRepository(categories),
            clock = clock,
            zoneId = zone,
        )

    private fun command(
        id: String? = null,
        amount: String = "150",
        spentAt: LocalDate? = today,
        categoryId: Long = cafe.id,
    ) = SaveExpenseCommand(
        id = id,
        amountInput = amount,
        spentAt = spentAt,
        name = "Латте",
        categoryId = categoryId,
        categoryAssignmentSource = CategoryAssignmentSource.FALLBACK,
    )
}

private fun category(id: Long, archived: Boolean = false) = Category(
    id = id,
    code = "CAFE",
    name = "Кафе",
    normalizedName = "кафе",
    color = "#E65100",
    icon = "cafe",
    isBuiltin = true,
    archivedAt = if (archived) Instant.EPOCH else null,
)

private fun expense(id: String, categoryId: Long) = Expense(
    id = id,
    amount = com.olegbelyanin.expensetracker.model.Money(15_000),
    spentAt = Instant.parse("2026-09-03T00:00:00Z"),
    name = "Латте",
    normalizedName = "латте",
    categoryId = categoryId,
    locationId = null,
    comment = null,
    categoryAssignmentSource = CategoryAssignmentSource.FALLBACK,
    dedupKey = "user:$id",
)

private class FakeCategoryRepository(private val categories: List<Category>) : CategoryRepository {
    override suspend fun getActiveCategories(): List<Category> = categories.filter { it.isActive }

    override fun observeActiveCategories(): Flow<List<Category>> = MutableStateFlow(categories.filter { it.isActive })

    override fun observeAll(): Flow<List<Category>> = MutableStateFlow(categories)

    override fun observeArchivedCategories(): Flow<List<Category>> =
        MutableStateFlow(categories.filter { !it.isActive })

    override suspend fun findById(id: Long): Category? = categories.find { it.id == id }

    override suspend fun requireFallback(): Category = categories.first { it.isFallback }

    override suspend fun createUserCategory(name: String, color: String, icon: String): Category = error("not used")

    override suspend fun updateUserCategory(id: Long, name: String, color: String, icon: String): Category =
        error("not used")

    override suspend fun archive(id: Long): Category = error("not used")

    override suspend fun restore(id: Long): Category = error("not used")
}

private class FakeExpenseRepository(vararg initial: Expense) : ExpenseRepository {
    private val stored = initial.associateBy { it.id }.toMutableMap()
    val persistCalls = mutableListOf<PersistExpenseRequest>()

    override suspend fun get(id: String): Expense? = stored[id]

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
            dedupKey = "user:${request.id}",
        )
        stored[expense.id] = expense
        return expense
    }

    override suspend fun delete(id: String) {
        stored.remove(id)
    }
}
