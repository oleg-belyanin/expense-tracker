package com.olegbelyanin.expensetracker.database.search

import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.ExpenseNameSuggestion
import com.olegbelyanin.expensetracker.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class NameSuggestionRankerTest {
    @Test
    fun groupsHistoryByNormalizedNameAndKeepsLatestDisplayName() {
        val older = Instant.parse("2026-07-01T00:00:00Z")
        val newer = Instant.parse("2026-09-04T00:00:00Z")
        val grouped = NameSuggestionRanker.fromExpenses(
            listOf(
                expense("e1", "кетостерил", "кетостер", older),
                expense("e2", "Кетостерил", "кетостер", newer),
                expense("e3", "Кетчуп", "кетчуп", newer),
            ),
        )
        assertEquals("Кетостерил", grouped.single { it.normalizedName == "кетостер" }.name)
        assertEquals(2, grouped.single { it.normalizedName == "кетостер" }.usageCount)
    }

    @Test
    fun prefersHistoryDisplayOverDictionaryStem() {
        val used = Instant.parse("2026-09-03T00:00:00Z")
        val merged = NameSuggestionRanker.merge(
            queryNormalized = "кет",
            history = listOf(
                ExpenseNameSuggestion("Кетостерил", "кетостер", 3, used, fromDictionary = false),
            ),
            dictionary = listOf(
                ExpenseNameSuggestion("Кетостер", "кетостер", 0, null, fromDictionary = true),
                ExpenseNameSuggestion("Кетчуп", "кетчуп", 0, null, fromDictionary = true),
            ),
            limit = 8,
        )
        assertEquals(listOf("Кетостерил", "Кетчуп"), merged.map { it.name })
    }

    @Test
    fun hidesExactQueryMatch() {
        val merged = NameSuggestionRanker.merge(
            queryNormalized = "кетчуп",
            history = listOf(
                ExpenseNameSuggestion("Кетчуп", "кетчуп", 1, Instant.parse("2026-07-25T00:00:00Z")),
            ),
            dictionary = emptyList(),
            limit = 8,
        )
        assertEquals(emptyList<String>(), merged.map { it.name })
    }

    @Test
    fun titleCasesDictionaryStem() {
        assertEquals("Кетостер", NameSuggestionRanker.titleCase("кетостер"))
        assertEquals("Apple Music", NameSuggestionRanker.titleCase("apple music"))
    }

    private fun expense(id: String, name: String, normalized: String, spentAt: Instant) = Expense(
        id = id,
        amount = Money(100),
        spentAt = spentAt,
        name = name,
        normalizedName = normalized,
        categoryId = 1,
        locationId = null,
        comment = null,
        categoryAssignmentSource = CategoryAssignmentSource.FALLBACK,
        dedupKey = id,
    )
}
