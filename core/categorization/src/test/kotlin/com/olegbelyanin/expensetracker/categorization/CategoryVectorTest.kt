package com.olegbelyanin.expensetracker.categorization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryVectorTest {
    private val config = CategorizationConfig(laplaceAlpha = 0.0, maxSeedStrength = 50.0)
    private val health = 4L
    private val food = 1L
    private val other = 10L
    private val active = setOf(health, food, other)

    @Test
    fun averagesKnownVectorsAndIgnoresMissing() {
        val ketosteril = CategoryVector(mapOf(health to 0.96, food to 0.01, other to 0.03))
        val tablets = CategoryVector(mapOf(health to 0.80, food to 0.05, other to 0.15))
        val averaged = CategoryVector.average(listOf(ketosteril, tablets))!!
        assertEquals(0.88, averaged.score(health), 1e-9)
        assertEquals(0.03, averaged.score(food), 1e-9)
        assertEquals(0.09, averaged.score(other), 1e-9)
        assertNull(CategoryVector.average(emptyList()))
    }

    @Test
    fun combinesNameAndLocationWithConfiguredWeights() {
        val name = CategoryVector(mapOf(health to 0.90, other to 0.10))
        val location = CategoryVector(mapOf(health to 0.20, other to 0.80))
        val combined = CategoryVector.combine(name, location, nameWeight = 2.0, locationWeight = 1.0)!!
        assertEquals(2.0 / 3.0, combined.score(health), 1e-9)
        assertEquals(1.0 / 3.0, combined.score(other), 1e-9)
        assertEquals(name, CategoryVector.combine(name, null, 2.0, 1.0))
        assertEquals(location, CategoryVector.combine(null, location, 2.0, 1.0))
    }

    @Test
    fun breaksScoreTiesByCategoryIdAsc() {
        val vector = CategoryVector(mapOf(other to 0.5, health to 0.5))
        assertEquals(health, vector.argMax())
    }

    @Test
    fun fromCountsUsesCappedSeedPriorAndUserCounts() {
        val counts = listOf(
            FeatureCount(health, CategoryVector.SOURCE_SEED, 100),
            FeatureCount(food, CategoryVector.SOURCE_USER, 10),
        )
        val vector = CategoryVector.fromCounts(counts, active, config)!!
        assertTrue(vector.score(health) > vector.score(food))
        val strongerUser = CategoryVector.fromCounts(
            listOf(
                FeatureCount(health, CategoryVector.SOURCE_SEED, 5),
                FeatureCount(food, CategoryVector.SOURCE_USER, 20),
            ),
            active,
            config,
        )!!
        assertEquals(food, strongerUser.argMax())
    }

    @Test
    fun locationEligibleUsesRawShareNotSmoothedProbability() {
        val clear = listOf(FeatureCount(health, CategoryVector.SOURCE_SEED, 2))
        val mixed = listOf(
            FeatureCount(food, CategoryVector.SOURCE_SEED, 4),
            FeatureCount(other, CategoryVector.SOURCE_SEED, 3),
            FeatureCount(health, CategoryVector.SOURCE_SEED, 1),
        )
        val eligibility = CategorizationConfig(minSeedSupport = 1, minSeedProbability = 0.70)
        assertTrue(CategoryVector.locationEligible(clear, eligibility))
        assertFalse(CategoryVector.locationEligible(mixed, eligibility))
    }

    @Test
    fun evenTwoWaySplitRequiresEqualCountsOfExactlyTwoCategories() {
        val tie = listOf(
            FeatureCount(health, CategoryVector.SOURCE_USER, 1),
            FeatureCount(food, CategoryVector.SOURCE_USER, 1),
        )
        assertEquals(setOf(health, food), CategoryVector.evenTwoWaySplit(tie))
        assertNull(
            CategoryVector.evenTwoWaySplit(
                listOf(
                    FeatureCount(health, CategoryVector.SOURCE_USER, 2),
                    FeatureCount(food, CategoryVector.SOURCE_USER, 1),
                ),
            ),
        )
        assertNull(
            CategoryVector.evenTwoWaySplit(
                listOf(
                    FeatureCount(health, CategoryVector.SOURCE_USER, 1),
                    FeatureCount(food, CategoryVector.SOURCE_USER, 1),
                    FeatureCount(other, CategoryVector.SOURCE_USER, 1),
                ),
            ),
        )
    }

    @Test
    fun newActiveCategoryAddsScoreDimensionWithoutMigratingCounts() {
        val counts = listOf(FeatureCount(health, CategoryVector.SOURCE_SEED, 10))
        val before = CategoryVector.fromCounts(counts, setOf(health, other), config)!!
        val stomatology = 8L
        val after = CategoryVector.fromCounts(counts, setOf(health, other, stomatology), config)!!
        assertEquals(setOf(health, other), before.scores.keys)
        assertEquals(setOf(health, other, stomatology), after.scores.keys)
        assertEquals(1.0, after.scores.values.sum(), 1e-9)
        assertEquals(0.0, after.score(stomatology), 1e-9)
        assertEquals(before.score(health), after.score(health), 1e-9)
    }

    @Test
    fun fromCountsReturnsNullForUnknownFeature() {
        assertNull(CategoryVector.fromCounts(emptyList(), active, config))
        assertNull(CategoryVector.fromCounts(listOf(FeatureCount(99, CategoryVector.SOURCE_SEED, 3)), active, config))
    }

    @Test
    fun transitionFromOdsRenormalizesAndPicksNewCategory() {
        val p0 = 50.0 / 51.0
        val p1 = 1.0 / 51.0
        val stomatology = 8L
        val base = CategoryVector(mapOf(health to p0, stomatology to p1))
        val adjusted = base.applyTransition(health, stomatology, margin = 0.1, epsilon = 1e-9)
        assertEquals(1.0, adjusted.scores.values.sum(), 1e-9)
        assertTrue(adjusted.score(stomatology) > adjusted.score(health))
        assertEquals(0.4995, adjusted.score(health), 1e-3)
        assertEquals(0.5005, adjusted.score(stomatology), 1e-3)
    }
}
