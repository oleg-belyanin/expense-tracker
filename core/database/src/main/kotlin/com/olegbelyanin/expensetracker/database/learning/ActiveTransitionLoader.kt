package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.categorization.CategoryTransitionLink
import com.olegbelyanin.expensetracker.categorization.FeatureTransition
import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.database.dao.LearningDao

internal object ActiveTransitionLoader {
    suspend fun load(
        learningDao: LearningDao,
        keywordIdsByFeature: Map<KeywordFeature, Long>,
    ): List<FeatureTransition> {
        val keywordIds = keywordIdsByFeature.values.distinct()
        if (keywordIds.isEmpty()) return emptyList()
        val rows = learningDao.activeTransitionsForKeywords(keywordIds).associateBy { it.keywordId }
        return keywordIdsByFeature.mapNotNull { (feature, keywordId) ->
            val row = rows[keywordId] ?: return@mapNotNull null
            FeatureTransition(feature, row.fromCategoryId, row.toCategoryId)
        }
    }

    suspend fun loadOpenCategoryLinks(learningDao: LearningDao): List<CategoryTransitionLink> =
        learningDao.activeCategoryTransitions().map { row ->
            CategoryTransitionLink(row.fromCategoryId, row.toCategoryId, row.createdAt)
        }
}
