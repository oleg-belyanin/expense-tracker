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

data class CategoryTransitionLink(
    val fromCategoryId: Long,
    val toCategoryId: Long,
    val createdAt: Long = 0L,
)

data class CategorizationSnapshot(
    val fallbackCategoryId: Long,
    val activeCategoryIds: Set<Long>,
    val localExact: ExactMatch? = null,
    val seedExact: ExactMatch? = null,
    val categoryAliasId: Long? = null,
    val featureVectors: Map<KeywordFeature, CategoryVector> = emptyMap(),
    val locationVector: CategoryVector? = null,
    /** false — место слишком смешанное, чтобы выбирать категорию, но вектор ещё нужен для dropdown. */
    val locationEligible: Boolean = true,
    /** Две категории с сырой долей 50/50 по месту; иначе пусто. */
    val locationTiedCategoryIds: Set<Long> = emptySet(),
    val transitions: List<FeatureTransition> = emptyList(),
    val categoryTransitions: List<CategoryTransitionLink> = emptyList(),
)

data class CategorizationLookup(val query: CategorizationQuery, val snapshot: CategorizationSnapshot)

fun interface CategorizationCatalog {
    suspend fun lookup(name: String, locationName: String?): CategorizationLookup
}
