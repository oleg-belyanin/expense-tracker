package com.olegbelyanin.expensetracker.categorization

import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.KeywordKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorizationEngineTest {
    private val engine = CategorizationEngine()
    private val cafe = 2L
    private val health = 4L
    private val other = 10L
    private val active = setOf(cafe, health, other)
    private val latte = KeywordFeature("латт", KeywordKind.WORD)
    private val nameVector = CategoryVector(mapOf(cafe to 0.70, health to 0.10, other to 0.20))

    @Test
    fun localExactBeatsAliasSeedAndVector() {
        val result = engine.categorize(
            query("латт", latte),
            snapshot(
                localExact = ExactMatch(health, "explicit"),
                seedExact = ExactMatch(cafe, CategoryVector.SOURCE_SEED),
                categoryAliasId = cafe,
                featureVectors = mapOf(latte to nameVector),
            ),
        )
        assertEquals(health, result.selectedCategoryId)
        assertEquals(CategoryAssignmentSource.EXACT_USER, result.source)
        assertFalse(result.usedFallback)
        assertEquals(cafe, result.orderedCandidates.first().categoryId)
    }

    @Test
    fun archivedLocalExactIsIgnored() {
        val result = engine.categorize(
            query("латт", latte),
            snapshot(
                localExact = ExactMatch(99, "explicit"),
                seedExact = ExactMatch(cafe, CategoryVector.SOURCE_SEED),
                featureVectors = mapOf(latte to nameVector),
            ),
        )
        assertEquals(cafe, result.selectedCategoryId)
        assertEquals(CategoryAssignmentSource.PROBABILISTIC, result.source)
    }

    @Test
    fun categoryAliasBeatsSeedExactAndVector() {
        val result = engine.categorize(
            query("каф", KeywordFeature("каф", KeywordKind.WORD)),
            snapshot(
                seedExact = ExactMatch(health, CategoryVector.SOURCE_SEED),
                categoryAliasId = cafe,
                featureVectors = mapOf(KeywordFeature("каф", KeywordKind.WORD) to nameVector),
            ),
        )
        assertEquals(cafe, result.selectedCategoryId)
        assertEquals(CategoryAssignmentSource.PROBABILISTIC, result.source)
    }

    @Test
    fun createdStomatologyCategoryAliasesPurchaseOverHealthSeed() {
        val stomatology = 8L
        val feature = KeywordFeature("стоматолог", KeywordKind.WORD)
        val healthHeavy = CategoryVector(mapOf(health to 0.90, stomatology to 0.05, other to 0.05))
        val result = engine.categorize(
            query("стоматолог", feature),
            snapshot(
                activeCategoryIds = active + stomatology,
                seedExact = ExactMatch(health, CategoryVector.SOURCE_SEED),
                categoryAliasId = stomatology,
                featureVectors = mapOf(feature to healthHeavy),
            ),
        )
        assertEquals(stomatology, result.selectedCategoryId)
        assertEquals(CategoryAssignmentSource.PROBABILISTIC, result.source)
    }

    @Test
    fun archivedAliasIsIgnored() {
        val result = engine.categorize(
            query("каф"),
            snapshot(categoryAliasId = 99, seedExact = ExactMatch(health, CategoryVector.SOURCE_SEED)),
        )
        assertEquals(health, result.selectedCategoryId)
    }

    @Test
    fun seedExactBeatsAverageVector() {
        val groceries = CategoryVector(mapOf(cafe to 0.10, health to 0.10, other to 0.80))
        val result = engine.categorize(
            query("латт", latte),
            snapshot(
                seedExact = ExactMatch(cafe, CategoryVector.SOURCE_SEED),
                featureVectors = mapOf(latte to groceries),
            ),
        )
        assertEquals(cafe, result.selectedCategoryId)
        assertEquals(other, result.orderedCandidates.first().categoryId)
    }

    @Test
    fun averageVectorSelectsTopScore() {
        val result = engine.categorize(
            query("латт", latte),
            snapshot(featureVectors = mapOf(latte to nameVector)),
        )
        assertEquals(cafe, result.selectedCategoryId)
        assertEquals(CategoryAssignmentSource.PROBABILISTIC, result.source)
        assertEquals(0.70, result.confidence, 1e-9)
        assertEquals(listOf(cafe, other, health), result.orderedCandidates.map { it.categoryId })
        assertEquals("латт", result.matchedFeatures.single().value)
        assertFalse(result.usedFallback)
    }

    @Test
    fun unknownFeaturesDoNotEnterDenominator() {
        val unknown = KeywordFeature("xyz", KeywordKind.WORD)
        val result = engine.categorize(
            query("латт xyz", latte, unknown),
            snapshot(featureVectors = mapOf(latte to nameVector)),
        )
        assertEquals(cafe, result.selectedCategoryId)
        assertEquals(1, result.matchedFeatures.size)
    }

    @Test
    fun duplicateFeaturesCountOnce() {
        val result = engine.categorize(
            query("каф каф", latte, latte),
            snapshot(featureVectors = mapOf(latte to nameVector)),
        )
        assertEquals(cafe, result.selectedCategoryId)
        assertEquals(1, result.matchedFeatures.size)
    }

    @Test
    fun locationCombinesAfterNameAverage() {
        val closeName = CategoryVector(mapOf(cafe to 0.55, health to 0.35, other to 0.10))
        val location = CategoryVector(mapOf(cafe to 0.05, health to 0.90, other to 0.05))
        val result = engine.categorize(
            CategorizationQuery("кетостер", listOf(latte), locationNormalized = "столичка"),
            snapshot(featureVectors = mapOf(latte to closeName), locationVector = location),
        )
        assertEquals(health, result.selectedCategoryId)
        assertTrue(result.matchedFeatures.any { it.source == CategorizationEngine.SOURCE_LOCATION })
    }

    @Test
    fun missingFeaturesFallBackToOther() {
        val result = engine.categorize(query(""), snapshot())
        assertEquals(other, result.selectedCategoryId)
        assertEquals(CategoryAssignmentSource.FALLBACK, result.source)
        assertTrue(result.usedFallback)
        assertEquals(0.0, result.confidence, 0.0)
        assertEquals(other, result.orderedCandidates.single().categoryId)
    }

    @Test
    fun appliesActiveTransitionBeforeAverage() {
        val stomatology = 8L
        val base = CategoryVector(mapOf(health to 50.0 / 51.0, stomatology to 1.0 / 51.0, other to 0.0, cafe to 0.0))
        val feature = KeywordFeature("стоматолог", KeywordKind.WORD)
        val result = engine.categorize(
            query("стоматолог", feature),
            snapshot(
                activeCategoryIds = active + stomatology,
                featureVectors = mapOf(feature to base),
                transitions = listOf(FeatureTransition(feature, health, stomatology)),
            ),
        )
        assertEquals(stomatology, result.selectedCategoryId)
    }

    @Test
    fun ineligibleLocationStillRanksWithoutSelecting() {
        val stomatology = 8L
        val location = CategoryVector(mapOf(health to 0.45, stomatology to 0.45, other to 0.10))
        val result = engine.categorize(
            CategorizationQuery("", emptyList(), locationNormalized = "гашека"),
            snapshot(
                activeCategoryIds = active + stomatology,
                locationVector = location,
                locationEligible = false,
                locationTiedCategoryIds = setOf(health, stomatology),
            ),
        )
        assertEquals(other, result.selectedCategoryId)
        assertTrue(result.usedFallback)
        assertEquals(listOf(health, stomatology, other), result.orderedCandidates.map { it.categoryId })
    }

    @Test
    fun locationTieWithLiveTransitSelectsTarget() {
        val stomatology = 8L
        val location = CategoryVector(mapOf(health to 0.45, stomatology to 0.45, other to 0.10))
        val result = engine.categorize(
            CategorizationQuery("", emptyList(), locationNormalized = "гашека"),
            snapshot(
                activeCategoryIds = active + stomatology,
                locationVector = location,
                locationEligible = false,
                locationTiedCategoryIds = setOf(health, stomatology),
                categoryTransitions = listOf(CategoryTransitionLink(health, stomatology, createdAt = 10)),
            ),
        )
        assertEquals(stomatology, result.selectedCategoryId)
        assertEquals(CategoryAssignmentSource.PROBABILISTIC, result.source)
        assertFalse(result.usedFallback)
        assertEquals(listOf(health, stomatology, other), result.orderedCandidates.map { it.categoryId })
    }

    @Test
    fun laterTransitWinsLocationTie() {
        val stomatology = 8L
        val location = CategoryVector(mapOf(health to 0.5, stomatology to 0.5, other to 0.0))
        val result = engine.categorize(
            CategorizationQuery("", emptyList(), locationNormalized = "гашека"),
            snapshot(
                activeCategoryIds = active + stomatology,
                locationVector = location,
                locationEligible = false,
                locationTiedCategoryIds = setOf(health, stomatology),
                categoryTransitions = listOf(
                    CategoryTransitionLink(health, stomatology, createdAt = 1),
                    CategoryTransitionLink(stomatology, health, createdAt = 2),
                ),
            ),
        )
        assertEquals(health, result.selectedCategoryId)
    }

    @Test
    fun unrelatedTransitDoesNotBreakLocationTie() {
        val stomatology = 8L
        val result = engine.categorize(
            CategorizationQuery("", emptyList(), locationNormalized = "гашека"),
            snapshot(
                activeCategoryIds = active + stomatology,
                locationVector = CategoryVector(mapOf(health to 0.45, stomatology to 0.45, other to 0.10)),
                locationEligible = false,
                locationTiedCategoryIds = setOf(health, stomatology),
                categoryTransitions = listOf(CategoryTransitionLink(cafe, health, createdAt = 10)),
            ),
        )
        assertEquals(other, result.selectedCategoryId)
        assertTrue(result.usedFallback)
    }

    @Test
    fun nameVectorBlocksLocationTransitTieBreak() {
        val stomatology = 8L
        val result = engine.categorize(
            query("латт", latte),
            snapshot(
                activeCategoryIds = active + stomatology,
                featureVectors = mapOf(latte to nameVector),
                locationVector = CategoryVector(mapOf(health to 0.5, stomatology to 0.5, other to 0.0)),
                locationEligible = false,
                locationTiedCategoryIds = setOf(health, stomatology),
                categoryTransitions = listOf(CategoryTransitionLink(health, stomatology, createdAt = 10)),
            ),
        )
        assertEquals(cafe, result.selectedCategoryId)
        assertEquals(listOf(cafe, other, health), result.orderedCandidates.map { it.categoryId })
    }

    private fun query(normalized: String, vararg features: KeywordFeature) =
        CategorizationQuery(normalized, features.toList())

    private fun snapshot(
        activeCategoryIds: Set<Long> = active,
        localExact: ExactMatch? = null,
        seedExact: ExactMatch? = null,
        categoryAliasId: Long? = null,
        featureVectors: Map<KeywordFeature, CategoryVector> = emptyMap(),
        locationVector: CategoryVector? = null,
        locationEligible: Boolean = true,
        locationTiedCategoryIds: Set<Long> = emptySet(),
        transitions: List<FeatureTransition> = emptyList(),
        categoryTransitions: List<CategoryTransitionLink> = emptyList(),
    ) = CategorizationSnapshot(
        fallbackCategoryId = other,
        activeCategoryIds = activeCategoryIds,
        localExact = localExact,
        seedExact = seedExact,
        categoryAliasId = categoryAliasId,
        featureVectors = featureVectors,
        locationVector = locationVector,
        locationEligible = locationEligible,
        locationTiedCategoryIds = locationTiedCategoryIds,
        transitions = transitions,
        categoryTransitions = categoryTransitions,
    )
}
