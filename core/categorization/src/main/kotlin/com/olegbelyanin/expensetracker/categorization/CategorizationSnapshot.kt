package com.olegbelyanin.expensetracker.categorization

data class CategorizationQuery(
    val normalizedName: String,
    val features: List<KeywordFeature>,
    val locationNormalized: String? = null,
)

data class ExactMatch(val categoryId: Long, val source: String) {
    val isSeed: Boolean get() = source == CategoryVector.SOURCE_SEED
}

data class FeatureTransition(val feature: KeywordFeature, val fromCategoryId: Long, val toCategoryId: Long)

data class CategorizationSnapshot(
    val fallbackCategoryId: Long,
    val activeCategoryIds: Set<Long>,
    val localExact: ExactMatch? = null,
    val seedExact: ExactMatch? = null,
    val categoryAliasId: Long? = null,
    val featureVectors: Map<KeywordFeature, CategoryVector> = emptyMap(),
    val locationVector: CategoryVector? = null,
    val transitions: List<FeatureTransition> = emptyList(),
)

data class CategorizationLookup(val query: CategorizationQuery, val snapshot: CategorizationSnapshot)

fun interface CategorizationCatalog {
    suspend fun lookup(name: String, locationName: String?): CategorizationLookup
}
