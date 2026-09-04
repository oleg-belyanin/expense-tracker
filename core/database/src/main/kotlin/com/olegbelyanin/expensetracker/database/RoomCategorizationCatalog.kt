package com.olegbelyanin.expensetracker.database

import com.olegbelyanin.expensetracker.categorization.CategorizationCatalog
import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import com.olegbelyanin.expensetracker.categorization.CategorizationLookup
import com.olegbelyanin.expensetracker.categorization.CategorizationQuery
import com.olegbelyanin.expensetracker.categorization.CategorizationSnapshot
import com.olegbelyanin.expensetracker.categorization.CategoryVector
import com.olegbelyanin.expensetracker.categorization.ExactMatch
import com.olegbelyanin.expensetracker.categorization.FeatureCount
import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.categorization.NormalizationResult
import com.olegbelyanin.expensetracker.categorization.PrefixCandidate
import com.olegbelyanin.expensetracker.categorization.PrefixFeatureResolver
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LocationCategoryStatEntity
import com.olegbelyanin.expensetracker.database.learning.ActiveTransitionLoader
import com.olegbelyanin.expensetracker.database.search.LikeQuery
import com.olegbelyanin.expensetracker.model.BuiltinCategories
import com.olegbelyanin.expensetracker.model.KeywordKind

class RoomCategorizationCatalog(
    private val database: AppDatabase,
    private val normalizer: TextNormalizer,
    private val config: CategorizationConfig = CategorizationConfig.DEFAULT,
) : CategorizationCatalog {
    override suspend fun lookup(name: String, locationName: String?): CategorizationLookup {
        val analysis = normalizer.analyze(name)
        val locationNormalized = locationName?.let { normalizer.normalizePlain(it) }?.ifEmpty { null }
        val active = database.categoryDao().getActive()
        val activeIds = active.map { it.id }.toSet()
        val fallback = database.categoryDao().findBuiltinByCode(BuiltinCategories.FALLBACK_CODE)
            ?: error("Fallback category is missing")
        val rule = analysis.normalizedName.takeIf { it.isNotEmpty() }
            ?.let { database.learningDao().findExactRule(it) }
        val localExact = rule?.takeIf { it.source != CategoryVector.SOURCE_SEED && it.categoryId in activeIds }
            ?.let { ExactMatch(it.categoryId, it.source) }
        val seedExact = rule?.takeIf { it.source == CategoryVector.SOURCE_SEED && it.categoryId in activeIds }
            ?.let { ExactMatch(it.categoryId, it.source) }
        val aliasId = analysis.normalizedName.takeIf { it.isNotEmpty() }?.let { normalized ->
            active.firstOrNull { it.normalizedName == normalized }?.id
        }
        val featureVectors = linkedMapOf<KeywordFeature, CategoryVector>()
        val keywordIdsByFeature = linkedMapOf<KeywordFeature, Long>()
        val resolvedFeatures = linkedSetOf<KeywordFeature>()
        resolveTokens(analysis, name).forEach { token ->
            val resolved = resolveKeyword(token, activeIds) ?: return@forEach
            resolvedFeatures += resolved.feature
            keywordIdsByFeature[resolved.feature] = resolved.keywordId
            featureVectors[resolved.feature] = resolved.vector
        }
        val location = locationNormalized?.let { loadLocation(it, activeIds) }
        return CategorizationLookup(
            query = CategorizationQuery(
                analysis.normalizedName,
                resolvedFeatures.toList().ifEmpty { analysis.features },
                locationNormalized,
            ),
            snapshot = CategorizationSnapshot(
                fallbackCategoryId = fallback.id,
                activeCategoryIds = activeIds,
                localExact = localExact,
                seedExact = seedExact,
                categoryAliasId = aliasId,
                featureVectors = featureVectors,
                locationVector = location?.vector,
                locationEligible = location?.eligible ?: false,
                locationTiedCategoryIds = location?.tiedCategoryIds.orEmpty(),
                transitions = ActiveTransitionLoader.load(database.learningDao(), keywordIdsByFeature),
                categoryTransitions = ActiveTransitionLoader.loadOpenCategoryLinks(database.learningDao()),
            ),
        )
    }

    private fun resolveTokens(analysis: NormalizationResult, rawName: String): List<KeywordFeature> {
        val tokens = linkedSetOf<KeywordFeature>()
        analysis.features.forEach { tokens += it }
        normalizer.plainTokens(rawName).forEach { token ->
            tokens += KeywordFeature(token, KeywordKind.WORD)
        }
        return tokens.toList()
    }

    private suspend fun resolveKeyword(token: KeywordFeature, activeIds: Set<Long>): ResolvedKeyword? {
        val kind = token.kind.name.lowercase()
        val exact = database.keywordDao().find(kind, token.value)
        if (exact != null) return vectorFor(token, exact, activeIds)
        val stem = PrefixFeatureResolver.lookupStem(token.value) ?: return null
        val candidates = database.keywordDao().findByKindAndPrefix(kind, LikeQuery.prefix(stem))
        val scored = candidates.mapNotNull { keyword ->
            val counts = database.learningDao().statsForKeyword(keyword.id).toFeatureCounts()
            val vector = CategoryVector.fromCounts(counts, activeIds, config) ?: return@mapNotNull null
            val top = vector.argMax() ?: return@mapNotNull null
            PrefixCandidate(
                value = keyword.value,
                topCategoryId = top,
                support = counts.sumOf { it.count },
                payload = keyword to vector,
            )
        }
        val chosen = PrefixFeatureResolver.choose(token.value, scored) ?: return null
        val (keyword, vector) = chosen.payload
        return ResolvedKeyword(KeywordFeature(keyword.value, token.kind), keyword.id, vector)
    }

    private suspend fun vectorFor(
        feature: KeywordFeature,
        keyword: KeywordEntity,
        activeIds: Set<Long>,
    ): ResolvedKeyword? {
        val counts = database.learningDao().statsForKeyword(keyword.id).toFeatureCounts()
        val vector = CategoryVector.fromCounts(counts, activeIds, config) ?: return null
        return ResolvedKeyword(feature, keyword.id, vector)
    }

    private data class ResolvedKeyword(
        val feature: KeywordFeature,
        val keywordId: Long,
        val vector: CategoryVector,
    )

    private suspend fun loadLocation(normalized: String, activeIds: Set<Long>): LocationLookup? {
        val location = database.locationDao().findByNormalizedName(normalized) ?: return null
        val counts = database.learningDao().statsForLocation(location.id).toLocationCounts()
            .filter { it.categoryId in activeIds && it.count > 0 }
        if (counts.isEmpty()) return null
        val vector = CategoryVector.fromCounts(counts, activeIds, config) ?: return null
        return LocationLookup(
            vector = vector,
            eligible = CategoryVector.locationEligible(counts, config),
            tiedCategoryIds = CategoryVector.evenTwoWaySplit(counts).orEmpty(),
        )
    }

    private data class LocationLookup(
        val vector: CategoryVector,
        val eligible: Boolean,
        val tiedCategoryIds: Set<Long>,
    )

    private fun List<KeywordCategoryStatEntity>.toFeatureCounts(): List<FeatureCount> =
        map { FeatureCount(it.categoryId, it.source, it.observationCount) }

    private fun List<LocationCategoryStatEntity>.toLocationCounts(): List<FeatureCount> =
        map { FeatureCount(it.categoryId, it.source, it.observationCount) }
}
