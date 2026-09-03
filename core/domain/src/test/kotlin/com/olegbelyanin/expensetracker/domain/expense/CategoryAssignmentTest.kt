package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryAssignmentTest {
    @Test
    fun userPickIsAlwaysExplicit() {
        val source = CategoryAssignment.sourceForSave(
            userPicked = true,
            suggestionSource = CategoryAssignmentSource.PROBABILISTIC,
            originalSource = CategoryAssignmentSource.FALLBACK,
        )
        assertEquals(CategoryAssignmentSource.EXPLICIT, source)
    }

    @Test
    fun acceptedSuggestionKeepsEngineSource() {
        val source = CategoryAssignment.sourceForSave(
            userPicked = false,
            suggestionSource = CategoryAssignmentSource.PROBABILISTIC,
            originalSource = null,
        )
        assertEquals(CategoryAssignmentSource.PROBABILISTIC, source)
    }

    @Test
    fun editWithoutNewPickKeepsOriginalSource() {
        val source = CategoryAssignment.sourceForSave(
            userPicked = false,
            suggestionSource = null,
            originalSource = CategoryAssignmentSource.PROBABILISTIC,
        )
        assertEquals(CategoryAssignmentSource.PROBABILISTIC, source)
    }

    @Test
    fun emptyFormFallsBack() {
        val source = CategoryAssignment.sourceForSave(
            userPicked = false,
            suggestionSource = null,
            originalSource = null,
        )
        assertEquals(CategoryAssignmentSource.FALLBACK, source)
    }
}
