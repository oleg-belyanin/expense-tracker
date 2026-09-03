package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.categorization.KeywordTransition
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.AppDatabase
import com.olegbelyanin.expensetracker.database.dao.KeywordDao
import com.olegbelyanin.expensetracker.database.dao.LearningDao
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.ExactCategoryRuleEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LocationCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextKeywordEntity
import java.util.UUID

internal class LearningWriter(
    private val learningDao: LearningDao,
    private val keywordDao: KeywordDao,
    private val normalizer: TextNormalizer,
    private val config: CategorizationConfig = CategorizationConfig.DEFAULT,
) {
    private val refresher = TransitionRefresher(learningDao, config)

    constructor(database: AppDatabase, normalizer: TextNormalizer) : this(
        database.learningDao(),
        database.keywordDao(),
        normalizer,
    )

    suspend fun apply(
        expenseId: String,
        normalizedName: String,
        rawName: String,
        categoryId: Long,
        locationId: Long?,
        proposedCategoryId: Long?,
        plan: LearningPlan,
        now: Long,
        activeCategoryIds: Set<Long> = emptySet(),
    ) {
        if (plan.writeLearning) {
            val existing = learningDao.findExampleByExpenseId(expenseId)
            if (existing != null) {
                retract(existing)
            }
            val features = normalizer.analyze(rawName).features
            val keywordIds = features.map { requireKeyword(it) }
            val exampleId = existing?.id ?: UUID.randomUUID().toString()
            learningDao.upsertExample(
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
            learningDao.deleteExampleKeywords(exampleId)
            keywordIds.forEach { keywordId ->
                learningDao.insertExampleKeyword(
                    LearningExampleKeywordEntity(exampleId, keywordId),
                )
            }
            contribute(keywordIds, categoryId, locationId)
            val fromId = plan.transitionFromCategoryId
            val before = fromId?.let { snapshotKeywordCategories(keywordIds) }
            if (LearningPlanner.shouldWriteNameContext(normalizedName)) {
                writeNameContext(normalizedName, categoryId, plan.feedbackType, keywordIds, now)
            }
            if (fromId != null && fromId != categoryId) {
                recordTransition(
                    fromCategoryId = fromId,
                    toCategoryId = categoryId,
                    keywordIds = keywordIds,
                    before = before.orEmpty(),
                    after = snapshotKeywordCategories(keywordIds),
                    now = now,
                )
            }
            refresher.refresh(keywordIds, activeCategoryIds, now)
        }
        if (plan.writeExactRule) {
            ensureExactRule(normalizedName, categoryId, plan.feedbackType, now)
        }
    }

    private suspend fun retract(example: LearningExampleEntity) {
        val keywordIds = learningDao.exampleKeywordIds(example.id)
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

    private suspend fun snapshotKeywordCategories(keywordIds: List<Long>): Map<Long, Set<Long>> =
        keywordIds.distinct().associateWith { keywordId ->
            learningDao.contextCategoryIdsForKeyword(keywordId).toSet()
        }

    private suspend fun recordTransition(
        fromCategoryId: Long,
        toCategoryId: Long,
        keywordIds: List<Long>,
        before: Map<Long, Set<Long>>,
        after: Map<Long, Set<Long>>,
        now: Long,
    ) {
        val transitionId = UUID.randomUUID().toString()
        learningDao.insertTransition(
            CategoryTransitionEntity(
                id = transitionId,
                fromCategoryId = fromCategoryId,
                toCategoryId = toCategoryId,
                createdAt = now,
            ),
        )
        val moved = KeywordTransition.fullyTransitioned(
            keywordIds = keywordIds,
            fromCategoryId = fromCategoryId,
            toCategoryId = toCategoryId,
            categoriesBefore = before,
            categoriesAfter = after,
        )
        moved.forEach { keywordId ->
            replaceActiveKeyword(transitionId, keywordId, now)
        }
        if (learningDao.countActiveKeywords(transitionId) == 0L) {
            learningDao.closeTransition(transitionId, now)
        }
    }

    private suspend fun replaceActiveKeyword(transitionId: String, keywordId: Long, now: Long) {
        val existing = learningDao.findActiveByKeyword(keywordId)
        if (existing != null && existing.transitionId != transitionId) {
            learningDao.deactivateTransitionKeyword(existing.transitionId, keywordId, now)
            if (learningDao.countActiveKeywords(existing.transitionId) == 0L) {
                learningDao.closeTransition(existing.transitionId, now)
            }
        }
        learningDao.upsertTransitionKeyword(
            CategoryTransitionKeywordEntity(
                transitionId = transitionId,
                keywordId = keywordId,
                active = true,
                deactivatedAt = null,
            ),
        )
    }

    private suspend fun writeNameContext(
        normalizedName: String,
        categoryId: Long,
        source: String,
        keywordIds: List<Long>,
        now: Long,
    ) {
        learningDao.upsertNameContext(
            NameCategoryContextEntity(
                normalizedName = normalizedName,
                categoryId = categoryId,
                source = source,
                updatedAt = now,
            ),
        )
        learningDao.deleteNameContextKeywords(normalizedName)
        keywordIds.distinct().forEach { keywordId ->
            learningDao.insertNameContextKeyword(
                NameCategoryContextKeywordEntity(normalizedName, keywordId),
            )
        }
    }

    private suspend fun ensureExactRule(normalizedName: String, categoryId: Long, source: String, now: Long) {
        val existing = learningDao.findExactRule(normalizedName)
        val ruleSource = LearningPlanner.exactRuleSource(source)
        if (!LearningPlanner.shouldUpsertExactRule(
                normalizedName = normalizedName,
                existingCategoryId = existing?.categoryId,
                existingSource = existing?.source,
                categoryId = categoryId,
                ruleSource = ruleSource,
            )
        ) {
            return
        }
        learningDao.upsertExactRule(
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
        val existing = keywordDao.find(kind, feature.value)
        if (existing != null) return existing.id
        return keywordDao.insert(
            KeywordEntity(value = feature.value, kind = kind),
        )
    }

    private suspend fun adjustKeywordStat(keywordId: Long, categoryId: Long, delta: Int) {
        val current = learningDao.findKeywordStat(
            keywordId,
            categoryId,
            LearningPlanner.SOURCE_USER,
        )
        val next = (current?.observationCount ?: 0) + delta
        when {
            next <= 0 -> if (current != null) {
                learningDao.deleteKeywordStat(
                    keywordId,
                    categoryId,
                    LearningPlanner.SOURCE_USER,
                )
            }

            else -> learningDao.upsertKeywordStat(
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
        val current = learningDao.findLocationStat(
            locationId,
            categoryId,
            LearningPlanner.SOURCE_USER,
        )
        val next = (current?.observationCount ?: 0) + delta
        when {
            next <= 0 -> if (current != null) {
                learningDao.deleteLocationStat(
                    locationId,
                    categoryId,
                    LearningPlanner.SOURCE_USER,
                )
            }

            else -> learningDao.upsertLocationStat(
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
