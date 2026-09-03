package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningPlannerTest {
    private val first = LearningFingerprint("латте", categoryId = 2, locationId = 10)

    @Test
    fun firstExplicitSaveWritesExactRule() {
        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = first,
            source = CategoryAssignmentSource.EXPLICIT,
            interactive = true,
            proposedCategoryId = 2,
        )
        assertTrue(plan.writeLearning)
        assertTrue(plan.writeExactRule)
        assertEquals(LearningPlanner.EXPLICIT, plan.feedbackType)
        assertNull(plan.transitionFromCategoryId)
    }

    @Test
    fun firstSaveCorrectingProposalWritesExactRule() {
        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = first,
            source = CategoryAssignmentSource.EXPLICIT,
            interactive = true,
            proposedCategoryId = 4,
        )
        assertTrue(plan.writeExactRule)
        assertEquals(LearningPlanner.CORRECTION, plan.feedbackType)
        assertEquals(4L, plan.transitionFromCategoryId)
    }

    @Test
    fun acceptedExactUserRuleDoesNotWriteExactRule() {
        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = first,
            source = CategoryAssignmentSource.EXACT_USER,
            interactive = true,
            proposedCategoryId = 2,
        )
        assertTrue(plan.writeLearning)
        assertFalse(plan.writeExactRule)
        assertEquals(LearningPlanner.AUTO_ACCEPTED, plan.feedbackType)
    }

    @Test
    fun acceptedFallbackWritesLearningButNotExactRule() {
        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = first,
            source = CategoryAssignmentSource.FALLBACK,
            interactive = true,
            proposedCategoryId = 2,
        )
        assertTrue(plan.writeLearning)
        assertFalse(plan.writeExactRule)
        assertEquals(LearningPlanner.AUTO_ACCEPTED, plan.feedbackType)
        assertTrue(LearningPlanner.shouldWriteNameContext(first.normalizedName))
    }

    @Test
    fun emptyNormalizedNameDoesNotWriteNameContext() {
        assertFalse(LearningPlanner.shouldWriteNameContext(""))
    }

    @Test
    fun newInteractiveSaveWritesLearningAndBumpsLocation() {
        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = first,
            source = CategoryAssignmentSource.PROBABILISTIC,
            interactive = true,
        )
        assertTrue(plan.writeLearning)
        assertEquals(10L, plan.bumpLocationId)
        assertNull(plan.unbumpLocationId)
        assertEquals(LearningPlanner.AUTO_ACCEPTED, plan.feedbackType)
        assertFalse(plan.writeExactRule)
        assertNull(plan.transitionFromCategoryId)
    }

    @Test
    fun resaveWithoutChangesDoesNotBumpCounters() {
        val plan = LearningPlanner.plan(
            hadExpense = true,
            previous = first,
            next = first,
            source = CategoryAssignmentSource.PROBABILISTIC,
            interactive = true,
        )
        assertFalse(plan.writeLearning)
        assertNull(plan.bumpLocationId)
        assertNull(plan.unbumpLocationId)
        assertFalse(plan.writeExactRule)
    }

    @Test
    fun explicitResaveEnsuresRuleWithoutLearningWrite() {
        val plan = LearningPlanner.plan(
            hadExpense = true,
            previous = first,
            next = first,
            source = CategoryAssignmentSource.EXPLICIT,
            interactive = true,
        )
        assertFalse(plan.writeLearning)
        assertNull(plan.bumpLocationId)
        assertTrue(plan.writeExactRule)
    }

    @Test
    fun categoryChangeIsCorrectionWithTransition() {
        val next = first.copy(categoryId = 4)
        val plan = LearningPlanner.plan(
            hadExpense = true,
            previous = first,
            next = next,
            source = CategoryAssignmentSource.EXPLICIT,
            interactive = true,
        )
        assertTrue(plan.writeLearning)
        assertEquals(LearningPlanner.CORRECTION, plan.feedbackType)
        assertTrue(plan.writeExactRule)
        assertEquals(2L, plan.transitionFromCategoryId)
        assertNull(plan.bumpLocationId)
    }

    @Test
    fun locationChangeBumpsNewAndUnbumpsOld() {
        val next = first.copy(locationId = 11)
        val plan = LearningPlanner.plan(
            hadExpense = true,
            previous = first,
            next = next,
            source = CategoryAssignmentSource.FALLBACK,
            interactive = true,
        )
        assertTrue(plan.writeLearning)
        assertFalse(plan.writeExactRule)
        assertEquals(11L, plan.bumpLocationId)
        assertEquals(10L, plan.unbumpLocationId)
    }

    @Test
    fun upgradesSeedRuleToUserSource() {
        assertTrue(
            LearningPlanner.shouldUpsertExactRule(
                normalizedName = "латт",
                existingCategoryId = 2,
                existingSource = LearningPlanner.SOURCE_SEED,
                categoryId = 2,
                ruleSource = LearningPlanner.EXPLICIT,
            ),
        )
        assertFalse(
            LearningPlanner.shouldUpsertExactRule(
                normalizedName = "латт",
                existingCategoryId = 2,
                existingSource = LearningPlanner.EXPLICIT,
                categoryId = 2,
                ruleSource = LearningPlanner.EXPLICIT,
            ),
        )
        assertFalse(LearningPlanner.shouldUpsertExactRule("", 2, LearningPlanner.EXPLICIT, 2, LearningPlanner.EXPLICIT))
    }

    @Test
    fun importDoesNotLearn() {
        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = first,
            source = CategoryAssignmentSource.FALLBACK,
            interactive = false,
        )
        assertFalse(plan.writeLearning)
        assertFalse(plan.writeExactRule)
        assertEquals(10L, plan.bumpLocationId)
    }

    @Test
    fun recalcDoesNotLearnEvenWhenCategoryChanges() {
        val plan = LearningPlanner.plan(
            hadExpense = true,
            previous = first,
            next = first.copy(categoryId = 4),
            source = CategoryAssignmentSource.PROBABILISTIC,
            interactive = false,
        )
        assertFalse(plan.writeLearning)
        assertFalse(plan.writeExactRule)
        assertNull(plan.transitionFromCategoryId)
        assertNull(plan.bumpLocationId)
    }
}
