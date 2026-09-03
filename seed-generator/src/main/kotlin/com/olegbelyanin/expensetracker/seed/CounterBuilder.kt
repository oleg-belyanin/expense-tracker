package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.categorization.TextNormalizer

object CounterBuilder {
    fun build(rows: List<DatasetRow>, normalizer: TextNormalizer): TrainStatistics {
        val keywords = linkedMapOf<KeywordFeature, MutableMap<String, Int>>()
        val locations = linkedMapOf<String, MutableLocation>()
        val names = linkedMapOf<String, MutableMap<String, Int>>()
        rows.forEach { row ->
            val analysis = normalizer.analyze(row.name)
            analysis.features.forEach { feature ->
                increment(keywords.getOrPut(feature) { linkedMapOf() }, row.categoryCode)
            }
            if (analysis.normalizedName.isNotEmpty()) {
                increment(names.getOrPut(analysis.normalizedName) { linkedMapOf() }, row.categoryCode)
            }
            val rawLocation = row.location ?: return@forEach
            val normalizedLocation = normalizer.normalizePlain(rawLocation)
            if (normalizedLocation.isEmpty()) return@forEach
            val bucket = locations.getOrPut(normalizedLocation) { MutableLocation(rawLocation) }
            increment(bucket.counts, row.categoryCode)
        }
        return TrainStatistics(
            keywordCounts = keywords,
            locationCounts = locations.mapValues { (_, bucket) ->
                LocationAggregate(bucket.displayName, bucket.counts)
            },
            nameCategoryCounts = names,
        )
    }

    private fun increment(counts: MutableMap<String, Int>, categoryCode: String) {
        counts[categoryCode] = (counts[categoryCode] ?: 0) + 1
    }

    private class MutableLocation(val displayName: String) {
        val counts = linkedMapOf<String, Int>()
    }
}
