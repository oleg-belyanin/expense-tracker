package com.olegbelyanin.expensetracker.categorization

/**
 * Полный переход слова A → B и завершение транзита по `Pbase` (§9.5 AD-CAT-001).
 */
object KeywordTransition {
    fun isFullTransition(
        categoriesBefore: Set<Long>,
        categoriesAfter: Set<Long>,
        fromCategoryId: Long,
        toCategoryId: Long,
    ): Boolean = categoriesBefore == setOf(fromCategoryId) && categoriesAfter == setOf(toCategoryId)

    fun fullyTransitioned(
        keywordIds: Collection<Long>,
        fromCategoryId: Long,
        toCategoryId: Long,
        categoriesBefore: Map<Long, Set<Long>>,
        categoriesAfter: Map<Long, Set<Long>>,
    ): List<Long> = keywordIds.distinct().filter { keywordId ->
        isFullTransition(
            categoriesBefore[keywordId].orEmpty(),
            categoriesAfter[keywordId].orEmpty(),
            fromCategoryId,
            toCategoryId,
        )
    }

    fun shouldDeactivate(base: CategoryVector, fromCategoryId: Long, toCategoryId: Long): Boolean =
        base.score(toCategoryId) > base.score(fromCategoryId)
}
