package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.AppDatabase
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionEntity
import com.olegbelyanin.expensetracker.database.entities.ExactCategoryRuleEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LocationCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextKeywordEntity
import java.util.UUID

internal class LearningWriter(private val database: AppDatabase, private val normalizer: TextNormalizer) {
    suspend fun apply(
        expenseId: String,
        normalizedName: String,
        rawName: String,
        categoryId: Long,
        locationId: Long?,
        proposedCategoryId: Long?,
        plan: LearningPlan,
        now: Long,
    ) {
        if (plan.writeLearning) {
            val existing = database.learningDao().findExampleByExpenseId(expenseId)
            if (existing != null) {
                retract(existing)
            }
            val features = normalizer.analyze(rawName).features
            val keywordIds = features.map { requireKeyword(it) }
            val exampleId = existing?.id ?: UUID.randomUUID().toString()
            database.learningDao().upsertExample(
                LearningExampleEntity(
                    id = exampleId,
                    expenseId = expenseId,
                    normalizedName = normalizedName,
                    categoryId = categoryId,
                    proposedCategoryId = proposedCategoryId,
                    locationId = locationId,
                    feedbackType = plan.feedbackType,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            database.learningDao().deleteExampleKeywords(exampleId)
            keywordIds.forEach { keywordId ->
                database.learningDao().insertExampleKeyword(
                    LearningExampleKeywordEntity(exampleId, keywordId),
                )
            }
            contribute(keywordIds, categoryId, locationId)
            writeNameContext(normalizedName, categoryId, plan.feedbackType, keywordIds, now)
            plan.transitionFromCategoryId?.let { fromId ->
                database.learningDao().insertTransition(
                    CategoryTransitionEntity(
                        id = UUID.randomUUID().toString(),
                        fromCategoryId = fromId,
                        toCategoryId = categoryId,
                        createdAt = now,
                    ),
                )
            }
        }
        if (plan.writeExactRule) {
            ensureExactRule(normalizedName, categoryId, plan.feedbackType, now)
        }
    }

    private suspend fun retract(example: LearningExampleEntity) {
        val keywordIds = database.learningDao().exampleKeywordIds(example.id)
        keywordIds.forEach { keywordId ->
            adjustKeywordStat(keywordId, example.categoryId, -1)
        }
        example.locationId?.let { locationId ->
            adjustLocationStat(locationId, example.categoryId, -1)
        }
    }

    private suspend fun contribute(keywordIds: List<Long>, categoryId: Long, locationId: Long?) {
        keywordIds.forEach { keywordId ->
            adjustKeywordStat(keywordId, categoryId, 1)
        }
        locationId?.let { adjustLocationStat(it, categoryId, 1) }
    }

    private suspend fun writeNameContext(
        normalizedName: String,
        categoryId: Long,
        source: String,
        keywordIds: List<Long>,
        now: Long,
    ) {
        database.learningDao().upsertNameContext(
            NameCategoryContextEntity(
                normalizedName = normalizedName,
                categoryId = categoryId,
                source = source,
                updatedAt = now,
            ),
        )
        database.learningDao().deleteNameContextKeywords(normalizedName)
        keywordIds.distinct().forEach { keywordId ->
            database.learningDao().insertNameContextKeyword(
                NameCategoryContextKeywordEntity(normalizedName, keywordId),
            )
        }
    }

    private suspend fun ensureExactRule(normalizedName: String, categoryId: Long, source: String, now: Long) {
        val existing = database.learningDao().findExactRule(normalizedName)
        if (existing != null && existing.categoryId == categoryId) return
        val ruleSource = if (source == LearningPlanner.CORRECTION) {
            LearningPlanner.CORRECTION
        } else {
            LearningPlanner.EXPLICIT
        }
        database.learningDao().upsertExactRule(
            ExactCategoryRuleEntity(
                normalizedName = normalizedName,
                categoryId = categoryId,
                source = ruleSource,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    private suspend fun requireKeyword(feature: KeywordFeature): Long {
        val kind = feature.kind.name.lowercase()
        val existing = database.keywordDao().find(kind, feature.value)
        if (existing != null) return existing.id
        return database.keywordDao().insert(
            KeywordEntity(value = feature.value, kind = kind),
        )
    }

    private suspend fun adjustKeywordStat(keywordId: Long, categoryId: Long, delta: Int) {
        val current = database.learningDao().findKeywordStat(
            keywordId,
            categoryId,
            LearningPlanner.SOURCE_USER,
        )
        val next = (current?.observationCount ?: 0) + delta
        when {
            next <= 0 -> if (current != null) {
                database.learningDao().deleteKeywordStat(
                    keywordId,
                    categoryId,
                    LearningPlanner.SOURCE_USER,
                )
            }

            else -> database.learningDao().upsertKeywordStat(
                KeywordCategoryStatEntity(
                    keywordId = keywordId,
                    categoryId = categoryId,
                    source = LearningPlanner.SOURCE_USER,
                    observationCount = next,
                ),
            )
        }
    }

    private suspend fun adjustLocationStat(locationId: Long, categoryId: Long, delta: Int) {
        val current = database.learningDao().findLocationStat(
            locationId,
            categoryId,
            LearningPlanner.SOURCE_USER,
        )
        val next = (current?.observationCount ?: 0) + delta
        when {
            next <= 0 -> if (current != null) {
                database.learningDao().deleteLocationStat(
                    locationId,
                    categoryId,
                    LearningPlanner.SOURCE_USER,
                )
            }

            else -> database.learningDao().upsertLocationStat(
                LocationCategoryStatEntity(
                    locationId = locationId,
                    categoryId = categoryId,
                    source = LearningPlanner.SOURCE_USER,
                    observationCount = next,
                ),
            )
        }
    }
}
