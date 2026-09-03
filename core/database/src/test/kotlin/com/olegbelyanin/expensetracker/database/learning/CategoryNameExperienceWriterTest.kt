package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.categorization.CategoryNameExperience
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryNameExperienceWriterTest {
    private val normalizer = TextNormalizer()

    @Test
    fun writeIfMissingAddsOneCategoryNameRowPerFeature() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        writer(learning, keywords).writeIfMissing(STOMATOLOGY, "Стоматология")
        val featureId = keywords.requireId("word", "стоматолог")
        assertEquals(
            1,
            learning.findKeywordStat(featureId, STOMATOLOGY, CategoryNameExperience.SOURCE)?.observationCount,
        )
        assertEquals(1L, learning.countKeywordStats(STOMATOLOGY, CategoryNameExperience.SOURCE))
        assertNull(learning.findExactRule("стоматолог"))
    }

    @Test
    fun writeIfMissingDoesNotDuplicateOnReinit() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        val writer = writer(learning, keywords)
        writer.writeIfMissing(STOMATOLOGY, "Стоматология")
        writer.writeIfMissing(STOMATOLOGY, "Ортодонтия и клиника")
        assertEquals(1L, learning.countKeywordStats(STOMATOLOGY, CategoryNameExperience.SOURCE))
        assertNull(keywords.find("word", "ортодонт"))
        assertEquals(
            1,
            learning.findKeywordStat(
                keywords.requireId("word", "стоматолог"),
                STOMATOLOGY,
                CategoryNameExperience.SOURCE,
            )?.observationCount,
        )
    }

    @Test
    fun replaceRemovesOldFeaturesAndWritesNewOnes() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        val writer = writer(learning, keywords)
        writer.writeIfMissing(STOMATOLOGY, "Стоматология")
        val dentistId = keywords.requireId("word", "стоматолог")
        writer.replace(STOMATOLOGY, "Ортодонтия")
        val orthoId = keywords.requireId("word", "ортодонт")
        assertNull(learning.findKeywordStat(dentistId, STOMATOLOGY, CategoryNameExperience.SOURCE))
        assertEquals(
            1,
            learning.findKeywordStat(orthoId, STOMATOLOGY, CategoryNameExperience.SOURCE)?.observationCount,
        )
        assertEquals(1L, learning.countKeywordStats(STOMATOLOGY, CategoryNameExperience.SOURCE))
        assertNull(learning.findExactRule("ортодонт"))
    }

    private fun writer(learning: FakeLearningDao, keywords: FakeKeywordDao) =
        CategoryNameExperienceWriter(learning, keywords, normalizer)

    companion object {
        private const val STOMATOLOGY = 8L
    }
}
