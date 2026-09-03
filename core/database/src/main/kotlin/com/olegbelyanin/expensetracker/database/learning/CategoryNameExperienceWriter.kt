package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.categorization.CategoryNameExperience
import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.AppDatabase
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordEntity

internal class CategoryNameExperienceWriter(
    private val database: AppDatabase,
    private val normalizer: TextNormalizer,
) {
    suspend fun replace(categoryId: Long, rawName: String) {
        delete(categoryId)
        write(categoryId, rawName)
    }

    suspend fun writeIfMissing(categoryId: Long, rawName: String) {
        val existing = database.learningDao().countKeywordStats(categoryId, CategoryNameExperience.SOURCE)
        if (existing > 0) return
        write(categoryId, rawName)
    }

    suspend fun delete(categoryId: Long) {
        database.learningDao().deleteKeywordStats(categoryId, CategoryNameExperience.SOURCE)
    }

    private suspend fun write(categoryId: Long, rawName: String) {
        CategoryNameExperience.features(normalizer, rawName).forEach { feature ->
            database.learningDao().upsertKeywordStat(
                KeywordCategoryStatEntity(
                    keywordId = requireKeyword(feature),
                    categoryId = categoryId,
                    source = CategoryNameExperience.SOURCE,
                    observationCount = 1,
                ),
            )
        }
    }

    private suspend fun requireKeyword(feature: KeywordFeature): Long {
        val kind = feature.kind.name.lowercase()
        val existing = database.keywordDao().find(kind, feature.value)
        if (existing != null) return existing.id
        return database.keywordDao().insert(
            KeywordEntity(value = feature.value, kind = kind),
        )
    }
}
