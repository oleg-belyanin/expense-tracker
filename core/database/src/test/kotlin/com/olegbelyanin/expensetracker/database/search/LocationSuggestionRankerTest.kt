package com.olegbelyanin.expensetracker.database.search

import com.olegbelyanin.expensetracker.model.Location
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class LocationSuggestionRankerTest {
    @Test
    fun ranksExactThenPrefixThenFtsAndRecency() {
        val older = Instant.parse("2026-08-01T00:00:00Z")
        val newer = Instant.parse("2026-09-01T00:00:00Z")
        val exact = location(1, "Магазин", "магазин", usage = 1, lastUsed = older)
        val prefix = location(2, "Магазин №2", "магазин 2", usage = 5, lastUsed = newer)
        val ftsOnly = location(3, "Пятёрочка", "пятерочка", usage = 9, lastUsed = newer)
        val prefixOlder = location(4, "Магнит", "магазин магнит", usage = 2, lastUsed = older)
        val merged = LocationSuggestionRanker.merge(
            queryNormalized = "магазин",
            prefixHits = listOf(prefix, exact, prefixOlder),
            ftsHits = listOf(ftsOnly, exact),
            limit = 8,
        )
        assertEquals(listOf(1L, 2L, 4L, 3L), merged.map { it.id })
    }

    @Test
    fun breaksTiesByUsageThenId() {
        val at = Instant.parse("2026-09-01T00:00:00Z")
        val a = location(8, "Ашан", "ашан", usage = 3, lastUsed = at)
        val b = location(3, "Ашан на Ленина", "ашан на ленина", usage = 3, lastUsed = at)
        val c = location(5, "Ашан Сити", "ашан сити", usage = 7, lastUsed = at)
        val merged = LocationSuggestionRanker.merge(
            queryNormalized = "аш",
            prefixHits = listOf(a, b, c),
            ftsHits = emptyList(),
            limit = 8,
        )
        assertEquals(listOf(5L, 3L, 8L), merged.map { it.id })
    }

    private fun location(id: Long, name: String, normalized: String, usage: Int, lastUsed: Instant) = Location(
        id = id,
        name = name,
        normalizedName = normalized,
        usageCount = usage,
        lastUsedAt = lastUsed,
        archivedAt = null,
    )
}
