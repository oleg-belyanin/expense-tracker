package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import com.olegbelyanin.expensetracker.categorization.CategorizationEngine
import com.olegbelyanin.expensetracker.categorization.CategorizationQuery
import com.olegbelyanin.expensetracker.categorization.CategorizationSnapshot
import com.olegbelyanin.expensetracker.categorization.CategoryNameExperience
import com.olegbelyanin.expensetracker.categorization.CategoryVector
import com.olegbelyanin.expensetracker.categorization.ExactMatch
import com.olegbelyanin.expensetracker.categorization.FeatureCount
import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.model.BuiltinCategories
import com.olegbelyanin.expensetracker.model.CategorizationResult
import com.olegbelyanin.expensetracker.model.KeywordKind

class SeedCatalog(
    snapshot: SeedSnapshot,
    private val catalog: CategoryCatalog,
    private val config: CategorizationConfig,
    private val normalizer: TextNormalizer = TextNormalizer(),
    private val engine: CategorizationEngine = CategorizationEngine(config),
) {
    private val featureCounts: Map<KeywordFeature, List<FeatureCount>> = buildFeatureCounts(snapshot)

    private val locationCounts: Map<String, List<FeatureCount>> =
        snapshot.locationStats.groupBy { normalizer.normalizePlain(it.location) }.mapValues { (_, rows) ->
            rows.map { FeatureCount(catalog.id(it.category_code), CategoryVector.SOURCE_SEED, it.count) }
        }

    private val exactRules: Map<String, Long> =
        snapshot.exactRules.associate { it.normalized_name to catalog.id(it.category_code) }

    fun categorize(name: String, location: String?): CategorizationResult {
        val analysis = normalizer.analyze(name)
        val locationNormalized = location?.let { normalizer.normalizePlain(it) }?.ifEmpty { null }
        val featureVectors = linkedMapOf<KeywordFeature, CategoryVector>()
        analysis.features.forEach { feature ->
            val counts = featureCounts[feature] ?: return@forEach
            val vector = CategoryVector.fromCounts(counts, catalog.activeIds, config) ?: return@forEach
            featureVectors[feature] = vector
        }
        val locationLookup = locationNormalized?.let { loadLocation(it) }
        val seedExact = analysis.normalizedName.takeIf { it.isNotEmpty() }?.let { exactRules[it] }
            ?.let { ExactMatch(it, CategoryVector.SOURCE_SEED) }
        return engine.categorize(
            CategorizationQuery(analysis.normalizedName, analysis.features, locationNormalized),
            CategorizationSnapshot(
                fallbackCategoryId = catalog.fallbackId,
                activeCategoryIds = catalog.activeIds,
                seedExact = seedExact,
                featureVectors = featureVectors,
                locationVector = locationLookup?.vector,
                locationEligible = locationLookup?.eligible ?: false,
                locationTiedCategoryIds = locationLookup?.tiedCategoryIds.orEmpty(),
            ),
        )
    }

    private data class LocationLookup(
        val vector: CategoryVector,
        val eligible: Boolean,
        val tiedCategoryIds: Set<Long>,
    )

    private fun loadLocation(normalized: String): LocationLookup? {
        val counts = locationCounts[normalized] ?: return null
        val vector = CategoryVector.fromCounts(counts, catalog.activeIds, config) ?: return null
        return LocationLookup(
            vector = vector,
            eligible = CategoryVector.locationEligible(counts, config),
            tiedCategoryIds = CategoryVector.evenTwoWaySplit(counts).orEmpty(),
        )
    }

    private fun buildFeatureCounts(snapshot: SeedSnapshot): Map<KeywordFeature, List<FeatureCount>> {
        val merged = linkedMapOf<KeywordFeature, MutableList<FeatureCount>>()
        snapshot.keywordStats.forEach { row ->
            merged.getOrPut(row.toFeature()) { mutableListOf() }.add(
                FeatureCount(catalog.id(row.category_code), CategoryVector.SOURCE_SEED, row.count),
            )
        }
        BuiltinCategories.all.forEach { spec ->
            val categoryId = catalog.idsByCode[spec.code] ?: return@forEach
            CategoryNameExperience.features(normalizer, spec.name).forEach { feature ->
                merged.getOrPut(feature) { mutableListOf() }.add(
                    FeatureCount(categoryId, CategoryNameExperience.SOURCE, 1),
                )
            }
        }
        return merged
    }

    private fun SeedKeywordStatDto.toFeature(): KeywordFeature =
        KeywordFeature(keyword, KeywordKind.valueOf(kind.uppercase()))
}
