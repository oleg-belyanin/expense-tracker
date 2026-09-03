package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.model.BuiltinCategories

data class DatasetRow(val name: String, val location: String?, val categoryCode: String)

data class LocationAggregate(val displayName: String, val counts: Map<String, Int>)

data class TrainStatistics(
    val keywordCounts: Map<KeywordFeature, Map<String, Int>>,
    val locationCounts: Map<String, LocationAggregate>,
    val nameCategoryCounts: Map<String, Map<String, Int>>,
)

data class CategoryCatalog(
    val idsByCode: Map<String, Long>,
    val codesById: Map<Long, String>,
    val fallbackCode: String,
    val fallbackId: Long,
    val activeIds: Set<Long>,
) {
    fun id(code: String): Long = idsByCode[code] ?: error("Unknown category_code=$code")

    fun code(id: Long): String = codesById[id] ?: error("Unknown category id=$id")

    companion object {
        fun fromCodes(codes: List<String>, fallbackCode: String = BuiltinCategories.FALLBACK_CODE): CategoryCatalog {
            val idsByCode = codes.mapIndexed { index, code -> code to (index + 1L) }.toMap()
            val fallbackId = idsByCode[fallbackCode] ?: error("Fallback $fallbackCode is missing")
            return CategoryCatalog(
                idsByCode = idsByCode,
                codesById = idsByCode.entries.associate { it.value to it.key },
                fallbackCode = fallbackCode,
                fallbackId = fallbackId,
                activeIds = idsByCode.values.toSet(),
            )
        }

        fun builtin(): CategoryCatalog = fromCodes(BuiltinCategories.all.map { it.code })
    }
}

data class ValidationMetrics(val rows: Int, val top1Hits: Int, val top3Hits: Int, val fallbackHits: Int) {
    val top1Accuracy: Double get() = ratio(top1Hits)
    val top3Recall: Double get() = ratio(top3Hits)
    val fallbackRate: Double get() = ratio(fallbackHits)

    private fun ratio(hits: Int): Double = if (rows == 0) 0.0 else hits.toDouble() / rows
}

data class SeedBuildResult(
    val snapshot: SeedSnapshot,
    val config: CategorizationConfigDocument,
    val metrics: ValidationMetrics,
    val mistakes: List<String> = emptyList(),
)

data class CategorizationConfigDocument(
    val engine: CategorizationConfig,
    val seedDataVersion: Int,
    val normalizerVersion: Int,
    val generatedAt: String,
)
