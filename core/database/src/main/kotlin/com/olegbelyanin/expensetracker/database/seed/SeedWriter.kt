package com.olegbelyanin.expensetracker.database.seed

import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.dao.KeywordDao
import com.olegbelyanin.expensetracker.database.dao.LearningDao
import com.olegbelyanin.expensetracker.database.entities.ExactCategoryRuleEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LocationCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextKeywordEntity
import com.olegbelyanin.expensetracker.database.learning.TransitionRefresher
import com.olegbelyanin.expensetracker.model.KeywordKind

internal class SeedWriter(
    private val learningDao: LearningDao,
    private val keywordDao: KeywordDao,
    private val normalizer: TextNormalizer,
    private val requireCategoryId: suspend (String) -> Long,
    private val requireLocationId: suspend (String) -> Long,
    private val activeCategoryIds: suspend () -> Set<Long>,
    private val refresher: TransitionRefresher = TransitionRefresher(learningDao),
) {
    suspend fun apply(snapshot: SeedSnapshot, now: Long) {
        replaceKeywordStats(snapshot.keywordStats)
        replaceLocationStats(snapshot.locationStats)
        replaceNameContexts(snapshot.contexts, now)
        replaceExactRules(snapshot.exactRules, now)
        refresher.refreshActive(activeCategoryIds(), now)
    }

    private suspend fun replaceKeywordStats(rows: List<SeedKeywordStatDto>) {
        learningDao.deleteKeywordStatsBySource(SeedImportRules.SOURCE_SEED)
        rows.forEach { row ->
            val kind = KeywordKind.valueOf(row.kind.uppercase())
            val keywordId = requireKeyword(kind, normalizer.normalizePlain(row.keyword))
            learningDao.upsertKeywordStat(
                KeywordCategoryStatEntity(
                    keywordId = keywordId,
                    categoryId = requireCategoryId(row.category_code),
                    source = SeedImportRules.SOURCE_SEED,
                    observationCount = row.count,
                ),
            )
        }
    }

    private suspend fun replaceLocationStats(rows: List<SeedLocationStatDto>) {
        learningDao.deleteLocationStatsBySource(SeedImportRules.SOURCE_SEED)
        rows.forEach { row ->
            learningDao.upsertLocationStat(
                LocationCategoryStatEntity(
                    locationId = requireLocationId(row.location),
                    categoryId = requireCategoryId(row.category_code),
                    source = SeedImportRules.SOURCE_SEED,
                    observationCount = row.count,
                ),
            )
        }
    }

    private suspend fun replaceNameContexts(rows: List<SeedNameContextDto>, now: Long) {
        val incoming = rows.map { it.normalized_name }.toSet()
        learningDao.nameContextNamesBySource(SeedImportRules.SOURCE_SEED).forEach { name ->
            if (name !in incoming) {
                learningDao.deleteNameContextKeywords(name)
                learningDao.deleteNameContextIfSource(name, SeedImportRules.SOURCE_SEED)
            }
        }
        rows.forEach { row ->
            val existing = learningDao.findNameContext(row.normalized_name)
            if (!SeedImportRules.shouldReplaceNameContext(existing?.source)) return@forEach
            val keywordIds = row.keywords.map { keyword ->
                requireKeyword(KeywordKind.WORD, normalizer.normalizePlain(keyword))
            }
            learningDao.upsertNameContext(
                NameCategoryContextEntity(
                    normalizedName = row.normalized_name,
                    categoryId = requireCategoryId(row.category_code),
                    source = SeedImportRules.SOURCE_SEED,
                    updatedAt = now,
                ),
            )
            learningDao.deleteNameContextKeywords(row.normalized_name)
            keywordIds.distinct().forEach { keywordId ->
                learningDao.insertNameContextKeyword(
                    NameCategoryContextKeywordEntity(row.normalized_name, keywordId),
                )
            }
        }
    }

    private suspend fun replaceExactRules(rows: List<SeedExactRuleDto>, now: Long) {
        val incoming = rows.map { it.normalized_name }.toSet()
        learningDao.exactRuleNamesBySource(SeedImportRules.SOURCE_SEED).forEach { name ->
            if (name !in incoming) {
                learningDao.deleteExactRuleIfSource(name, SeedImportRules.SOURCE_SEED)
            }
        }
        rows.forEach { row ->
            val existing = learningDao.findExactRule(row.normalized_name)
            if (!SeedImportRules.shouldReplaceExactRule(existing?.source)) return@forEach
            learningDao.upsertExactRule(
                ExactCategoryRuleEntity(
                    normalizedName = row.normalized_name,
                    categoryId = requireCategoryId(row.category_code),
                    source = SeedImportRules.SOURCE_SEED,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
        }
    }

    private suspend fun requireKeyword(kind: KeywordKind, value: String): Long {
        val existing = keywordDao.find(kind.name.lowercase(), value)
        if (existing != null) return existing.id
        return keywordDao.insert(KeywordEntity(value = value, kind = kind.name.lowercase()))
    }
}
