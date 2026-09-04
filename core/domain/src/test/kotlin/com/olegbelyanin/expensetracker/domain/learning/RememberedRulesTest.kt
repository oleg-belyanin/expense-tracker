package com.olegbelyanin.expensetracker.domain.learning

import com.olegbelyanin.expensetracker.domain.LearningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RememberedRulesTest {
    @Test
    fun exactCountUsesOnlyUserExactSources() {
        assertTrue(RememberedRules.countsExact(RememberedRules.EXPLICIT))
        assertTrue(RememberedRules.countsExact(RememberedRules.CORRECTION))
        assertFalse(RememberedRules.countsExact("seed"))
        assertFalse(RememberedRules.countsExact("auto_accepted"))
        assertEquals(2, RememberedRules.count(listOf("explicit", "seed", "correction", "seed")))
    }

    @Test
    fun userStatCountIgnoresSeedAndCategoryName() {
        assertTrue(RememberedRules.countsUserStat(RememberedRules.USER_STAT))
        assertFalse(RememberedRules.countsUserStat("seed"))
        assertFalse(RememberedRules.countsUserStat("category_name"))
        assertEquals(1, RememberedRules.countUserStats(listOf("user", "seed", "user", "category_name")))
    }

    @Test
    fun observeUseCaseForwardsRepositoryCounts() = runTest {
        val counts = RememberedRuleCounts(exactRules = 3, keywordRules = 8, locationRules = 2)
        val learning = FakeLearningRepository(counts)
        assertEquals(counts, ObserveRememberedRuleCountUseCase(learning)().first())
    }
}

private class FakeLearningRepository(counts: RememberedRuleCounts) : LearningRepository {
    private val counts = MutableStateFlow(counts)

    override fun observeRememberedRuleCounts(): Flow<RememberedRuleCounts> = counts
}
