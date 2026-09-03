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
    fun countsOnlyUserExactSources() {
        assertTrue(RememberedRules.counts(RememberedRules.EXPLICIT))
        assertTrue(RememberedRules.counts(RememberedRules.CORRECTION))
        assertFalse(RememberedRules.counts("seed"))
        assertFalse(RememberedRules.counts("auto_accepted"))
        assertEquals(2, RememberedRules.count(listOf("explicit", "seed", "correction", "seed")))
    }

    @Test
    fun observeUseCaseForwardsRepositoryCount() = runTest {
        val learning = FakeLearningRepository(3)
        assertEquals(3, ObserveRememberedRuleCountUseCase(learning)().first())
    }
}

private class FakeLearningRepository(count: Int) : LearningRepository {
    private val count = MutableStateFlow(count)

    override fun observeRememberedRuleCount(): Flow<Int> = count
}
