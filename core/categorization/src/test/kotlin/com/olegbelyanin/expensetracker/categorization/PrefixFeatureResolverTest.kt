package com.olegbelyanin.expensetracker.categorization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefixFeatureResolverTest {
    @Test
    fun matchesTypedPrefixAndLongerUnstemmedToken() {
        assertTrue(PrefixFeatureResolver.matches("кет", "кетостер"))
        assertTrue(PrefixFeatureResolver.matches("кет", "кетчуп"))
        assertTrue(PrefixFeatureResolver.matches("кетостерил", "кетостер"))
        assertTrue(!PrefixFeatureResolver.matches("кетч", "кетостер"))
    }

    @Test
    fun skipsAmbiguousPrefixAcrossCategories() {
        val chosen = PrefixFeatureResolver.choose(
            "кет",
            listOf(
                PrefixCandidate("кетостер", topCategoryId = HEALTH, support = 1, payload = "health"),
                PrefixCandidate("кетчуп", topCategoryId = GROCERIES, support = 1, payload = "food"),
            ),
        )
        assertNull(chosen)
    }

    @Test
    fun picksUniqueLongerKeyword() {
        val chosen = PrefixFeatureResolver.choose(
            "кетост",
            listOf(
                PrefixCandidate("кетостер", topCategoryId = HEALTH, support = 1, payload = "health"),
                PrefixCandidate("кетчуп", topCategoryId = GROCERIES, support = 4, payload = "food"),
            ),
        )
        assertEquals("кетостер", chosen?.value)
        assertEquals(HEALTH, chosen?.topCategoryId)
    }

    @Test
    fun ketchupPrefixSelectsGroceries() {
        val chosen = PrefixFeatureResolver.choose(
            "кетч",
            listOf(
                PrefixCandidate("кетостер", topCategoryId = HEALTH, support = 1, payload = "health"),
                PrefixCandidate("кетчуп", topCategoryId = GROCERIES, support = 1, payload = "food"),
            ),
        )
        assertEquals("кетчуп", chosen?.value)
    }

    @Test
    fun ignoresTooShortPrefix() {
        assertNull(PrefixFeatureResolver.lookupStem("к"))
        assertNull(
            PrefixFeatureResolver.choose(
                "к",
                listOf(PrefixCandidate("кофе", topCategoryId = 3, support = 5, payload = "x")),
            ),
        )
    }

    companion object {
        private const val HEALTH = 4L
        private const val GROCERIES = 2L
    }
}
