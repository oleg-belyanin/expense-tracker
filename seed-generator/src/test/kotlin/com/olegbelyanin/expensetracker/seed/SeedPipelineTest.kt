package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.model.KeywordKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedPipelineTest {
    private val normalizer = TextNormalizer()
    private val catalog = CategoryCatalog.builtin()

    @Test
    fun counterBuilderAggregatesKeywordsAndNormalizedLocations() {
        val stats = CounterBuilder.build(
            listOf(
                DatasetRow("Латте", "Шоколадница", "CAFE"),
                DatasetRow("Латте карамель", "шоколадница", "CAFE"),
                DatasetRow("Хлеб", "Пятёрочка", "GROCERIES"),
                DatasetRow("Губки", "Пятерочка", "HOME"),
            ),
            normalizer,
        )
        val latte = stats.keywordCounts.getValue(KeywordFeature("латт", KeywordKind.WORD))
        assertEquals(2, latte.getValue("CAFE"))
        val location = stats.locationCounts.getValue("шоколадница")
        assertEquals("Шоколадница", location.displayName)
        assertEquals(2, location.counts.getValue("CAFE"))
        val store = stats.locationCounts.getValue("пятерочка")
        assertEquals(1, store.counts.getValue("GROCERIES"))
        assertEquals(1, store.counts.getValue("HOME"))
    }

    @Test
    fun contextBuilderKeepsLastCategoryAndWordKeywords() {
        val contexts = ContextBuilder.build(
            listOf(
                DatasetRow("Врач", "Поликлиника", "HEALTH"),
                DatasetRow("Врач", "Поликлиника", "OTHER"),
            ),
            normalizer,
        )
        assertEquals(1, contexts.size)
        assertEquals("врач", contexts.single().normalized_name)
        assertEquals("OTHER", contexts.single().category_code)
        assertEquals(listOf("врач"), contexts.single().keywords)
    }

    @Test
    fun seedFilterDropsAmbiguousLowProbabilityFeatures() {
        val stats = CounterBuilder.build(
            listOf(
                DatasetRow("Хлеб", "Пятёрочка", "GROCERIES"),
                DatasetRow("Губки", "Пятёрочка", "HOME"),
                DatasetRow("Латте", "Шоколадница", "CAFE"),
                DatasetRow("Капучино", "Шоколадница", "CAFE"),
                DatasetRow("Эспрессо", "Шоколадница", "CAFE"),
            ),
            normalizer,
        )
        val config = CategorizationConfig(minSeedSupport = 2, minSeedProbability = 0.70)
        val snapshot = SeedFilter.apply(stats, config)
        assertTrue(snapshot.locationStats.none { it.location == "Пятёрочка" })
        assertTrue(snapshot.locationStats.any { it.location == "Шоколадница" && it.category_code == "CAFE" })
        assertFalse(SeedFilter.eligible(mapOf("GROCERIES" to 1, "HOME" to 1), config))
        assertTrue(SeedFilter.eligible(mapOf("CAFE" to 3), config))
    }

    @Test
    fun exactRulesCoverUnambiguousFullNamesOnly() {
        val rules = SeedFilter.exactRules(
            mapOf(
                "латт" to mapOf("CAFE" to 2),
                "ремонт" to mapOf("HOUSING" to 1, "HOME" to 1),
            ),
        )
        assertEquals(listOf(SeedExactRuleDto("латт", "CAFE")), rules)
    }
}
