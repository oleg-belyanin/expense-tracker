package com.olegbelyanin.expensetracker.categorization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordTransitionTest {
    private val health = 4L
    private val stomatology = 8L
    private val doctor = 1L
    private val dentist = 2L

    @Test
    fun dentistFullyMovesWhileDoctorOnlyExpands() {
        val before = mapOf(
            doctor to setOf(health),
            dentist to setOf(health),
        )
        val after = mapOf(
            doctor to setOf(health, stomatology),
            dentist to setOf(stomatology),
        )
        val moved = KeywordTransition.fullyTransitioned(
            keywordIds = listOf(doctor, dentist),
            fromCategoryId = health,
            toCategoryId = stomatology,
            categoriesBefore = before,
            categoriesAfter = after,
        )
        assertEquals(listOf(dentist), moved)
        assertFalse(
            KeywordTransition.isFullTransition(setOf(health), setOf(health, stomatology), health, stomatology),
        )
        assertTrue(
            KeywordTransition.isFullTransition(setOf(health), setOf(stomatology), health, stomatology),
        )
    }

    @Test
    fun missingContextsAreNotAFullTransition() {
        assertTrue(
            KeywordTransition.fullyTransitioned(
                keywordIds = listOf(dentist),
                fromCategoryId = health,
                toCategoryId = stomatology,
                categoriesBefore = emptyMap(),
                categoriesAfter = mapOf(dentist to setOf(stomatology)),
            ).isEmpty(),
        )
    }

    @Test
    fun deactivationUsesBaseProbabilityNotAdjustedScore() {
        val p0 = 50.0 / 51.0
        val p1 = 1.0 / 51.0
        val base = CategoryVector(mapOf(health to p0, stomatology to p1))
        val adjusted = base.applyTransition(health, stomatology, margin = 0.1, epsilon = 1e-9)
        assertTrue(adjusted.score(stomatology) > adjusted.score(health))
        assertFalse(KeywordTransition.shouldDeactivate(base, health, stomatology))
        assertTrue(
            KeywordTransition.shouldDeactivate(
                CategoryVector(mapOf(health to 0.4, stomatology to 0.6)),
                health,
                stomatology,
            ),
        )
        assertFalse(
            KeywordTransition.shouldDeactivate(
                CategoryVector(mapOf(health to 0.5, stomatology to 0.5)),
                health,
                stomatology,
            ),
        )
    }
}
