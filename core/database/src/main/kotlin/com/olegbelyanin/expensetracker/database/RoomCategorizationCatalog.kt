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
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.LocationCategoryStatEntity
import com.olegbelyanin.expensetracker.database.learning.ActiveTransitionLoader
import com.olegbelyanin.expensetracker.model.BuiltinCategories

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
        analysis.features.forEach { feature ->
            val keyword = database.keywordDao().find(feature.kind.name.lowercase(), feature.value) ?: return@forEach
            keywordIdsByFeature[feature] = keyword.id
            val counts = database.learningDao().statsForKeyword(keyword.id).toFeatureCounts()
            val vector = CategoryVector.fromCounts(counts, activeIds, config) ?: return@forEach
            featureVectors[feature] = vector
        }
        val locationVector = locationNormalized?.let { loadLocationVector(it, activeIds) }
        return CategorizationLookup(
            query = CategorizationQuery(analysis.normalizedName, analysis.features, locationNormalized),
            snapshot = CategorizationSnapshot(
                fallbackCategoryId = fallback.id,
                activeCategoryIds = activeIds,
                localExact = localExact,
                seedExact = seedExact,
                categoryAliasId = aliasId,
                featureVectors = featureVectors,
                locationVector = locationVector,
                transitions = ActiveTransitionLoader.load(database.learningDao(), keywordIdsByFeature),
            ),
        )
    }

    private suspend fun loadLocationVector(normalized: String, activeIds: Set<Long>): CategoryVector? {
        val location = database.locationDao().findByNormalizedName(normalized) ?: return null
        val counts = database.learningDao().statsForLocation(location.id).toLocationCounts()
        if (!CategoryVector.locationEligible(counts, config)) return null
        return CategoryVector.fromCounts(counts, activeIds, config)
    }

    private fun List<KeywordCategoryStatEntity>.toFeatureCounts(): List<FeatureCount> =
        map { FeatureCount(it.categoryId, it.source, it.observationCount) }

    private fun List<LocationCategoryStatEntity>.toLocationCounts(): List<FeatureCount> =
        map { FeatureCount(it.categoryId, it.source, it.observationCount) }
}
