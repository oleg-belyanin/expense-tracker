package com.olegbelyanin.expensetracker.database.seed

import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.ExactCategoryRuleEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextEntity
import com.olegbelyanin.expensetracker.database.learning.FakeKeywordDao
import com.olegbelyanin.expensetracker.database.learning.FakeLearningDao
import com.olegbelyanin.expensetracker.database.learning.LearningPlanner
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedImportRulesTest {
    @Test
    fun replacesMissingOrSeedExactRuleOnly() {
        assertTrue(SeedImportRules.shouldReplaceExactRule(null))
        assertTrue(SeedImportRules.shouldReplaceExactRule(SeedImportRules.SOURCE_SEED))
        assertFalse(SeedImportRules.shouldReplaceExactRule(LearningPlanner.EXPLICIT))
        assertFalse(SeedImportRules.shouldReplaceExactRule(LearningPlanner.CORRECTION))
    }

    @Test
    fun replacesMissingOrSeedNameContextOnly() {
        assertTrue(SeedImportRules.shouldReplaceNameContext(null))
        assertTrue(SeedImportRules.shouldReplaceNameContext(SeedImportRules.SOURCE_SEED))
        assertFalse(SeedImportRules.shouldReplaceNameContext(LearningPlanner.AUTO_ACCEPTED))
        assertFalse(SeedImportRules.shouldReplaceNameContext(LearningPlanner.EXPLICIT))
    }
}

class SeedWriterTest {
    private val normalizer = TextNormalizer()

    @Test
    fun firstApplyWritesSeedRows() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        writer(learning, keywords).apply(latteSnapshot(), now = 1_000L)
        val keywordId = keywords.requireId("word", "латт")
        assertEquals(3, learning.findKeywordStat(keywordId, CAFE, SeedImportRules.SOURCE_SEED)?.observationCount)
        assertEquals(CAFE, learning.findNameContext("латт")?.categoryId)
        assertEquals(SeedImportRules.SOURCE_SEED, learning.findNameContext("латт")?.source)
        assertEquals(CAFE, learning.findExactRule("латт")?.categoryId)
        assertEquals(SeedImportRules.SOURCE_SEED, learning.findExactRule("латт")?.source)
    }

    @Test
    fun secondApplyIsIdempotent() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        val writer = writer(learning, keywords)
        writer.apply(latteSnapshot(), now = 1_000L)
        writer.apply(latteSnapshot(), now = 2_000L)
        val keywordId = keywords.requireId("word", "латт")
        assertEquals(3, learning.findKeywordStat(keywordId, CAFE, SeedImportRules.SOURCE_SEED)?.observationCount)
        assertEquals(1_000L, learning.findExactRule("латт")?.createdAt)
        assertEquals(2_000L, learning.findExactRule("латт")?.updatedAt)
    }

    @Test
    fun doesNotOverwriteUserExactRuleOrContext() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        learning.upsertExactRule(
            ExactCategoryRuleEntity("латт", HEALTH, LearningPlanner.EXPLICIT, createdAt = 1, updatedAt = 1),
        )
        learning.upsertNameContext(
            NameCategoryContextEntity("латт", HEALTH, LearningPlanner.AUTO_ACCEPTED, updatedAt = 1),
        )
        writer(learning, keywords).apply(latteSnapshot(), now = 2_000L)
        assertEquals(HEALTH, learning.findExactRule("латт")?.categoryId)
        assertEquals(LearningPlanner.EXPLICIT, learning.findExactRule("латт")?.source)
        assertEquals(HEALTH, learning.findNameContext("латт")?.categoryId)
        assertEquals(LearningPlanner.AUTO_ACCEPTED, learning.findNameContext("латт")?.source)
    }

    @Test
    fun keepsUserStatsWhenReplacingSeedStats() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        val keywordId = keywords.require("word", "латт")
        learning.upsertKeywordStat(KeywordCategoryStatEntity(keywordId, HEALTH, LearningPlanner.SOURCE_USER, 7))
        writer(learning, keywords).apply(latteSnapshot(count = 4), now = 1_000L)
        assertEquals(7, learning.findKeywordStat(keywordId, HEALTH, LearningPlanner.SOURCE_USER)?.observationCount)
        assertEquals(4, learning.findKeywordStat(keywordId, CAFE, SeedImportRules.SOURCE_SEED)?.observationCount)
    }

    @Test
    fun removesStaleSeedRulesAndContexts() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        val writer = writer(learning, keywords)
        writer.apply(latteSnapshot(), now = 1_000L)
        writer.apply(SeedSnapshot(), now = 2_000L)
        assertNull(learning.findExactRule("латт"))
        assertNull(learning.findNameContext("латт"))
        assertNull(
            learning.findKeywordStat(keywords.requireId("word", "латт"), CAFE, SeedImportRules.SOURCE_SEED),
        )
    }

    @Test
    fun seedUpdateRechecksActiveTransitions() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        val dentist = keywords.require("word", "стоматолог")
        learning.insertTransition(CategoryTransitionEntity("t1", HEALTH, STOMATOLOGY, createdAt = 1))
        learning.upsertTransitionKeyword(CategoryTransitionKeywordEntity("t1", dentist, active = true))
        writer(learning, keywords).apply(
            SeedSnapshot(
                keywordStats = listOf(
                    SeedKeywordStatDto("стоматолог", category_code = "STOMATOLOGY", count = 50),
                ),
            ),
            now = 2_000L,
        )
        assertNull(learning.findActiveByKeyword(dentist))
        assertEquals(2_000L, learning.findTransition("t1")?.closedAt)
    }

    private fun writer(learning: FakeLearningDao, keywords: FakeKeywordDao) = SeedWriter(
        learningDao = learning,
        keywordDao = keywords,
        normalizer = normalizer,
        requireCategoryId = { code ->
            when (code) {
                "CAFE" -> CAFE
                "HEALTH" -> HEALTH
                "STOMATOLOGY" -> STOMATOLOGY
                else -> error(code)
            }
        },
        requireLocationId = { 10L },
        activeCategoryIds = { setOf(CAFE, HEALTH, STOMATOLOGY, OTHER) },
    )

    private fun latteSnapshot(count: Int = 3) = SeedSnapshot(
        keywordStats = listOf(SeedKeywordStatDto("латт", category_code = "CAFE", count = count)),
        contexts = listOf(SeedNameContextDto("латт", "CAFE", keywords = listOf("латт"))),
        exactRules = listOf(SeedExactRuleDto("латт", "CAFE")),
    )

    private companion object {
        const val CAFE = 2L
        const val HEALTH = 4L
        const val STOMATOLOGY = 8L
        const val OTHER = 10L
    }
}
