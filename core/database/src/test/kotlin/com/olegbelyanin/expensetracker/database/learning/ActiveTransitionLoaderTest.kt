package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.categorization.FeatureTransition
import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionKeywordEntity
import com.olegbelyanin.expensetracker.model.KeywordKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveTransitionLoaderTest {
    @Test
    fun loadsOnlyOpenActiveLinksForRequestedFeatures() = runTest {
        val learning = FakeLearningDao()
        val dentist = KeywordFeature("стоматолог", KeywordKind.WORD)
        val doctor = KeywordFeature("врач", KeywordKind.WORD)
        learning.insertTransition(CategoryTransitionEntity("t1", 4, 8, createdAt = 1))
        learning.insertTransition(CategoryTransitionEntity("t2", 4, 8, createdAt = 1, closedAt = 2))
        learning.upsertTransitionKeyword(CategoryTransitionKeywordEntity("t1", 2, active = true))
        learning.upsertTransitionKeyword(CategoryTransitionKeywordEntity("t2", 1, active = true))
        val loaded = ActiveTransitionLoader.load(
            learning,
            mapOf(dentist to 2L, doctor to 1L),
        )
        assertEquals(listOf(FeatureTransition(dentist, 4, 8)), loaded)
    }

    @Test
    fun emptyFeaturesLoadNothing() = runTest {
        assertTrue(ActiveTransitionLoader.load(FakeLearningDao(), emptyMap()).isEmpty())
    }
}
