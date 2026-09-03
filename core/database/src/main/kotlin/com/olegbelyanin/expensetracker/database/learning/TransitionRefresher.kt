package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import com.olegbelyanin.expensetracker.categorization.CategoryVector
import com.olegbelyanin.expensetracker.categorization.FeatureCount
import com.olegbelyanin.expensetracker.categorization.KeywordTransition
import com.olegbelyanin.expensetracker.database.dao.LearningDao

internal class TransitionRefresher(
    private val learningDao: LearningDao,
    private val config: CategorizationConfig = CategorizationConfig.DEFAULT,
) {
    suspend fun refreshActive(activeCategoryIds: Set<Long>, now: Long) {
        refresh(learningDao.activeTransitionKeywordIds(), activeCategoryIds, now)
    }

    suspend fun refresh(keywordIds: List<Long>, activeCategoryIds: Set<Long>, now: Long) {
        if (activeCategoryIds.isEmpty()) return
        keywordIds.distinct().forEach { keywordId ->
            val link = learningDao.findActiveByKeyword(keywordId) ?: return@forEach
            val transition = learningDao.findTransition(link.transitionId) ?: return@forEach
            if (transition.closedAt != null) return@forEach
            val counts = learningDao.statsForKeyword(keywordId).map { row ->
                FeatureCount(row.categoryId, row.source, row.observationCount)
            }
            val base = CategoryVector.fromCounts(counts, activeCategoryIds, config) ?: return@forEach
            if (!KeywordTransition.shouldDeactivate(base, transition.fromCategoryId, transition.toCategoryId)) {
                return@forEach
            }
            learningDao.deactivateTransitionKeyword(link.transitionId, keywordId, now)
            if (learningDao.countActiveKeywords(link.transitionId) == 0L) {
                learningDao.closeTransition(link.transitionId, now)
            }
        }
    }
}
