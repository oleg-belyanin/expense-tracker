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
        val name = nameVector(query, snapshot)
        val ranking = rankingVector(name, snapshot)
        val selected = select(snapshot, active, fallbackId, name)
        val usedFallback = selected.source == CategoryAssignmentSource.FALLBACK
        return CategorizationResult(
            selectedCategoryId = selected.categoryId,
            orderedCandidates = rank(ranking, fallbackId),
            source = selected.source,
            confidence = confidence(selected.categoryId, ranking, usedFallback),
            matchedFeatures = matchedFeatures(query, snapshot, ranking, name),
            usedFallback = usedFallback,
        )
    }

    private fun select(
        snapshot: CategorizationSnapshot,
        active: Set<Long>,
        fallbackId: Long,
        name: CategoryVector?,
    ): Selected {
        val local = snapshot.localExact?.takeIf { it.categoryId in active }
        if (local != null) return Selected(local.categoryId, CategoryAssignmentSource.EXACT_USER)
        val alias = snapshot.categoryAliasId?.takeIf { it in active }
        if (alias != null) return Selected(alias, CategoryAssignmentSource.PROBABILISTIC)
        val seed = snapshot.seedExact?.takeIf { it.categoryId in active }
        if (seed != null) return Selected(seed.categoryId, CategoryAssignmentSource.PROBABILISTIC)
        val transitWinner = locationTransitTieBreak(name, snapshot, active)
        if (transitWinner != null) {
            return Selected(transitWinner, CategoryAssignmentSource.PROBABILISTIC)
        }
        val selection = CategoryVector.combine(
            name,
            snapshot.locationVector.takeIf { snapshot.locationEligible },
            config.nameWeight,
            config.locationWeight,
        )
        val fromVector = selection?.argMax()?.takeIf { it in active }
        if (fromVector != null) return Selected(fromVector, CategoryAssignmentSource.PROBABILISTIC)
        return Selected(fallbackId, CategoryAssignmentSource.FALLBACK)
    }

    /**
     * Пустое имя и ничья места 50/50: живой категорийный транзит указывает,
     * какую из двух категорий пользователь уже выбрал руками (дух F-04).
     */
    private fun locationTransitTieBreak(
        name: CategoryVector?,
        snapshot: CategorizationSnapshot,
        active: Set<Long>,
    ): Long? {
        if (name != null) return null
        val tied = snapshot.locationTiedCategoryIds.filter { it in active }.toSet()
        if (tied.size != 2) return null
        val matching = snapshot.categoryTransitions.filter { link ->
            link.fromCategoryId in tied &&
                link.toCategoryId in tied &&
                link.fromCategoryId != link.toCategoryId
        }
        val latest = matching.maxByOrNull { it.createdAt } ?: return null
        return latest.toCategoryId.takeIf { it in active }
    }

    private fun nameVector(query: CategorizationQuery, snapshot: CategorizationSnapshot): CategoryVector? {
        val seen = linkedSetOf<KeywordFeature>()
        val adjusted = mutableListOf<CategoryVector>()
        query.features.forEach { feature ->
            if (!seen.add(feature)) return@forEach
            val base = snapshot.featureVectors[feature] ?: return@forEach
            adjusted += applyTransitions(feature, base, snapshot.transitions)
        }
        return CategoryVector.average(adjusted)
    }

    private fun rankingVector(name: CategoryVector?, snapshot: CategorizationSnapshot): CategoryVector? {
        val location = snapshot.locationVector
        return when {
            name != null && snapshot.locationEligible ->
                CategoryVector.combine(name, location, config.nameWeight, config.locationWeight)
            name != null -> name
            else -> location
        }
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
        ranking: CategoryVector?,
        name: CategoryVector?,
    ): List<MatchedFeature> {
        if (ranking == null && snapshot.locationVector == null) return emptyList()
        val seen = linkedSetOf<KeywordFeature>()
        val locationInRanking = snapshot.locationVector != null && (name == null || snapshot.locationEligible)
        val features = buildList {
            query.features.forEach { feature ->
                if (!seen.add(feature)) return@forEach
                if (feature !in snapshot.featureVectors) return@forEach
                add(MatchedFeature(feature.value, feature.kind, MatchedFeature.SOURCE_NAME))
            }
            val location = query.locationNormalized
            if (location != null && locationInRanking) {
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
