package com.olegbelyanin.expensetracker.categorization

import com.olegbelyanin.expensetracker.model.KeywordKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryNameExperienceTest {
    private val normalizer = TextNormalizer()

    @Test
    fun splitsMultiWordNameAndDropsStopWords() {
        val features = CategoryNameExperience.features(normalizer, "Стоматология и ортодонтия")
        assertEquals(setOf("стоматолог", "ортодонт"), features.map { it.value }.toSet())
        assertTrue(features.all { it.kind == KeywordKind.WORD })
    }

    @Test
    fun keepsOneRowPerFeature() {
        val features = CategoryNameExperience.features(normalizer, "Кафе кафе")
        assertEquals(1, features.size)
        assertEquals(normalizer.analyze("кафе").normalizedName, features.single().value)
    }

    @Test
    fun usesDedicatedSource() {
        assertEquals("category_name", CategoryNameExperience.SOURCE)
    }
}
