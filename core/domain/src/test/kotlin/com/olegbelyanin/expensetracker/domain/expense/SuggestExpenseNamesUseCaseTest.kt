package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.PersistExpenseRequest
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.ExpenseNameSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SuggestExpenseNamesUseCaseTest {
    @Test
    fun delegatesToRepositoryWithDefaultLimit() = runTest {
        val suggestion = ExpenseNameSuggestion(
            name = "Кетостерил",
            normalizedName = "кетостер",
            usageCount = 1,
            lastUsedAt = Instant.parse("2026-09-03T00:00:00Z"),
        )
        val repo = object : ExpenseRepository {
            var lastLimit = 0
            override suspend fun get(id: String) = null
            override suspend fun getAll() = emptyList<Expense>()
            override suspend fun findByDedupKey(dedupKey: String) = null
            override fun observeAll() = MutableStateFlow(emptyList<Expense>())
            override fun observeMatching(query: ExpenseSearchQuery): Flow<List<Expense>> = observeAll()
            override suspend fun suggestNames(query: String, limit: Int): List<ExpenseNameSuggestion> {
                lastLimit = limit
                return if (query == "Кет") listOf(suggestion) else emptyList()
            }
            override suspend fun persist(request: PersistExpenseRequest) = error("unused")
            override suspend fun delete(id: String) = Unit
            override suspend fun clearHistory() = 0
        }
        val result = SuggestExpenseNamesUseCase(repo)("Кет")
        assertEquals(listOf("Кетостерил"), result.map { it.name })
        assertEquals(SuggestExpenseNamesUseCase.DEFAULT_LIMIT, repo.lastLimit)
    }
}
