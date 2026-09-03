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
        assertEquals(11L, plan.bumpLocationId)
        assertEquals(10L, plan.unbumpLocationId)
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
}
