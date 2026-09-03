package com.olegbelyanin.expensetracker.categorization

import com.olegbelyanin.expensetracker.model.CategorizationCandidate
import com.olegbelyanin.expensetracker.model.CategorizationResult
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.KeywordKind
import com.olegbelyanin.expensetracker.model.MatchedFeature

/**
 * Чистый runtime §10.1 AD-CAT-001: local exact → alias → seed exact → векторы → fallback.
 * Dropdown всегда считается из вероятностного вектора, даже при точном правиле.
 */
class CategorizationEngine(private val config: CategorizationConfig = CategorizationConfig.DEFAULT) {
    fun categorize(query: CategorizationQuery, snapshot: CategorizationSnapshot): CategorizationResult {
        val active = snapshot.activeCategoryIds
        val fallbackId = snapshot.fallbackCategoryId
        val finalVector = probabilisticVector(query, snapshot)
        val candidates = rank(finalVector, fallbackId)
        val selected = select(snapshot, active, fallbackId, finalVector)
        val usedFallback = selected.source == CategoryAssignmentSource.FALLBACK
        return CategorizationResult(
            selectedCategoryId = selected.categoryId,
            orderedCandidates = candidates,
            source = selected.source,
            confidence = confidence(selected.categoryId, finalVector, usedFallback),
            matchedFeatures = matchedFeatures(query, snapshot, finalVector),
            usedFallback = usedFallback,
        )
    }

    private fun select(
        snapshot: CategorizationSnapshot,
        active: Set<Long>,
        fallbackId: Long,
        finalVector: CategoryVector?,
    ): Selected {
        val local = snapshot.localExact?.takeIf { it.categoryId in active }
        if (local != null) return Selected(local.categoryId, CategoryAssignmentSource.EXACT_USER)
        val alias = snapshot.categoryAliasId?.takeIf { it in active }
        if (alias != null) return Selected(alias, CategoryAssignmentSource.PROBABILISTIC)
        val seed = snapshot.seedExact?.takeIf { it.categoryId in active }
        if (seed != null) return Selected(seed.categoryId, CategoryAssignmentSource.PROBABILISTIC)
        val fromVector = finalVector?.argMax()?.takeIf { it in active }
        if (fromVector != null) return Selected(fromVector, CategoryAssignmentSource.PROBABILISTIC)
        return Selected(fallbackId, CategoryAssignmentSource.FALLBACK)
    }

    private fun probabilisticVector(query: CategorizationQuery, snapshot: CategorizationSnapshot): CategoryVector? {
        val seen = linkedSetOf<KeywordFeature>()
        val adjusted = mutableListOf<CategoryVector>()
        query.features.forEach { feature ->
            if (!seen.add(feature)) return@forEach
            val base = snapshot.featureVectors[feature] ?: return@forEach
            adjusted += applyTransitions(feature, base, snapshot.transitions)
        }
        val name = CategoryVector.average(adjusted)
        return CategoryVector.combine(name, snapshot.locationVector, config.nameWeight, config.locationWeight)
    }

    private fun applyTransitions(
        feature: KeywordFeature,
        base: CategoryVector,
        transitions: List<FeatureTransition>,
    ): CategoryVector {
        var current = base
        transitions.filter { it.feature == feature }.forEach { transition ->
            current = current.applyTransition(
                fromCategoryId = transition.fromCategoryId,
                toCategoryId = transition.toCategoryId,
                margin = config.transitionMargin,
                epsilon = config.transitionEpsilon,
            )
        }
        return current
    }

    private fun rank(vector: CategoryVector?, fallbackId: Long): List<CategorizationCandidate> {
        if (vector == null) return listOf(CategorizationCandidate(fallbackId, 0.0))
        return vector.scores.entries
            .sortedWith(compareByDescending<Map.Entry<Long, Double>> { it.value }.thenBy { it.key })
            .map { CategorizationCandidate(it.key, it.value) }
    }

    private fun confidence(selectedId: Long, vector: CategoryVector?, usedFallback: Boolean): Double {
        if (usedFallback) return 0.0
        if (vector == null) return 1.0
        return vector.score(selectedId)
    }

    private fun matchedFeatures(
        query: CategorizationQuery,
        snapshot: CategorizationSnapshot,
        finalVector: CategoryVector?,
    ): List<MatchedFeature> {
        if (finalVector == null && snapshot.locationVector == null) return emptyList()
        val seen = linkedSetOf<KeywordFeature>()
        val features = buildList {
            query.features.forEach { feature ->
                if (!seen.add(feature)) return@forEach
                if (feature !in snapshot.featureVectors) return@forEach
                add(MatchedFeature(feature.value, feature.kind, MatchedFeature.SOURCE_NAME))
            }
            val location = query.locationNormalized
            if (location != null && snapshot.locationVector != null) {
                add(MatchedFeature(location, KeywordKind.PHRASE, MatchedFeature.SOURCE_LOCATION))
            }
        }
        return features
    }

    private data class Selected(val categoryId: Long, val source: CategoryAssignmentSource)

    companion object {
        const val SOURCE_NAME = MatchedFeature.SOURCE_NAME
        const val SOURCE_LOCATION = MatchedFeature.SOURCE_LOCATION
    }
}
