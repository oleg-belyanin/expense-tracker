package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import com.olegbelyanin.expensetracker.categorization.KeywordFeature

object SeedFilter {
    fun apply(stats: TrainStatistics, config: CategorizationConfig): SeedSnapshot {
        val keywords = stats.keywordCounts
            .filter { (_, counts) -> eligible(counts, config) }
            .flatMap { (feature, counts) ->
                counts.map { (code, count) -> feature.toDto(code, count) }
            }
            .sortedWith(compareBy({ it.kind }, { it.keyword }, { it.category_code }))
        val locations = stats.locationCounts
            .filter { (_, aggregate) -> eligible(aggregate.counts, config) }
            .flatMap { (_, aggregate) ->
                aggregate.counts.map { (code, count) ->
                    SeedLocationStatDto(aggregate.displayName, code, count)
                }
            }
            .sortedWith(compareBy({ it.location }, { it.category_code }))
        return SeedSnapshot(
            keywordStats = keywords,
            locationStats = locations,
            exactRules = exactRules(stats.nameCategoryCounts),
        )
    }

    fun exactRules(nameCategoryCounts: Map<String, Map<String, Int>>): List<SeedExactRuleDto> = nameCategoryCounts
        .mapNotNull { (normalizedName, counts) ->
            val support = counts.values.sum()
            val unique = counts.filter { it.value > 0 }
            if (support >= 1 && unique.size == 1) {
                SeedExactRuleDto(normalizedName, unique.keys.single())
            } else {
                null
            }
        }
        .sortedBy { it.normalized_name }

    fun eligible(counts: Map<String, Int>, config: CategorizationConfig): Boolean {
        val support = counts.values.sum()
        if (support < config.minSeedSupport) return false
        val maxCount = counts.values.maxOrNull() ?: return false
        return maxCount.toDouble() / support >= config.minSeedProbability
    }

    private fun KeywordFeature.toDto(categoryCode: String, count: Int) = SeedKeywordStatDto(
        keyword = value,
        kind = kind.name.lowercase(),
        category_code = categoryCode,
        count = count,
    )
}
