package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.database.dao.ActiveKeywordTransitionRow
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
import com.olegbelyanin.expensetracker.domain.learning.RememberedRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeKeywordDao : KeywordDao {
    private val rows = mutableListOf<KeywordEntity>()
    private var nextId = 1L

    override suspend fun find(kind: String, value: String): KeywordEntity? =
        rows.find { it.kind == kind && it.value == value }

    override suspend fun getAll(): List<KeywordEntity> = rows.toList()

    override suspend fun insert(entity: KeywordEntity): Long {
        val id = nextId++
        rows += entity.copy(id = id)
        return id
    }

    suspend fun require(kind: String, value: String): Long {
        find(kind, value)?.let { return it.id }
        return insert(KeywordEntity(value = value, kind = kind))
    }

    fun requireId(kind: String, value: String): Long = rows.first { it.kind == kind && it.value == value }.id
}

internal class FakeLearningDao : LearningDao {
    private val examples = mutableMapOf<String, LearningExampleEntity>()
    private val exampleKeywords = mutableMapOf<String, MutableSet<Long>>()
    private val keywordStats = mutableMapOf<Triple<Long, Long, String>, KeywordCategoryStatEntity>()
    private val locationStats = mutableMapOf<Triple<Long, Long, String>, LocationCategoryStatEntity>()
    private val nameContexts = mutableMapOf<String, NameCategoryContextEntity>()
    private val nameContextKeywords = mutableMapOf<String, MutableSet<Long>>()
    private val exactRules = mutableMapOf<String, ExactCategoryRuleEntity>()
    private val transitionKeywords = mutableMapOf<Pair<String, Long>, CategoryTransitionKeywordEntity>()
    val transitions = mutableListOf<CategoryTransitionEntity>()
    private val userExactRuleCount = MutableStateFlow(0L)

    override suspend fun findExampleByExpenseId(expenseId: String): LearningExampleEntity? =
        examples.values.find { it.expenseId == expenseId }

    override suspend fun findExampleById(id: String): LearningExampleEntity? = examples[id]

    override suspend fun detachExamplesFromExpenses() {
        examples.replaceAll { _, example -> example.copy(expenseId = null) }
    }

    override suspend fun upsertExample(entity: LearningExampleEntity) {
        examples[entity.id] = entity
    }

    override suspend fun exampleKeywordIds(exampleId: String): List<Long> =
        exampleKeywords[exampleId].orEmpty().toList()

    override suspend fun deleteExampleKeywords(exampleId: String) {
        exampleKeywords.remove(exampleId)
    }

    override suspend fun insertExampleKeyword(entity: LearningExampleKeywordEntity) {
        exampleKeywords.getOrPut(entity.learningExampleId) { mutableSetOf() }.add(entity.keywordId)
    }

    override suspend fun findKeywordStat(
        keywordId: Long,
        categoryId: Long,
        source: String,
    ): KeywordCategoryStatEntity? = keywordStats[Triple(keywordId, categoryId, source)]

    override suspend fun upsertKeywordStat(entity: KeywordCategoryStatEntity) {
        keywordStats[Triple(entity.keywordId, entity.categoryId, entity.source)] = entity
    }

    override suspend fun deleteKeywordStat(keywordId: Long, categoryId: Long, source: String) {
        keywordStats.remove(Triple(keywordId, categoryId, source))
    }

    override suspend fun countKeywordStats(categoryId: Long, source: String): Long =
        keywordStats.values.count { it.categoryId == categoryId && it.source == source }.toLong()

    override suspend fun deleteKeywordStats(categoryId: Long, source: String) {
        keywordStats.entries.removeIf { it.value.categoryId == categoryId && it.value.source == source }
    }

    override suspend fun findLocationStat(
        locationId: Long,
        categoryId: Long,
        source: String,
    ): LocationCategoryStatEntity? = locationStats[Triple(locationId, categoryId, source)]

    override suspend fun upsertLocationStat(entity: LocationCategoryStatEntity) {
        locationStats[Triple(entity.locationId, entity.categoryId, entity.source)] = entity
    }

    override suspend fun deleteLocationStat(locationId: Long, categoryId: Long, source: String) {
        locationStats.remove(Triple(locationId, categoryId, source))
    }

    override suspend fun findNameContext(normalizedName: String): NameCategoryContextEntity? =
        nameContexts[normalizedName]

    override suspend fun upsertNameContext(entity: NameCategoryContextEntity) {
        nameContexts[entity.normalizedName] = entity
    }

    override suspend fun nameContextKeywordIds(normalizedName: String): List<Long> =
        nameContextKeywords[normalizedName].orEmpty().toList()

    override suspend fun deleteNameContextKeywords(normalizedName: String) {
        nameContextKeywords.remove(normalizedName)
    }

    override suspend fun insertNameContextKeyword(entity: NameCategoryContextKeywordEntity) {
        nameContextKeywords.getOrPut(entity.normalizedName) { mutableSetOf() }.add(entity.keywordId)
    }

    override suspend fun statsForKeyword(keywordId: Long): List<KeywordCategoryStatEntity> =
        keywordStats.values.filter { it.keywordId == keywordId }

    override suspend fun statsForLocation(locationId: Long): List<LocationCategoryStatEntity> =
        locationStats.values.filter { it.locationId == locationId }

    override suspend fun findExactRule(normalizedName: String): ExactCategoryRuleEntity? = exactRules[normalizedName]

    override fun observeUserExactRuleCount(): Flow<Long> = userExactRuleCount

    override suspend fun upsertExactRule(entity: ExactCategoryRuleEntity) {
        exactRules[entity.normalizedName] = entity
        refreshRememberedCount()
    }

    override suspend fun deleteKeywordStatsBySource(source: String) {
        keywordStats.entries.removeIf { it.value.source == source }
    }

    override suspend fun deleteLocationStatsBySource(source: String) {
        locationStats.entries.removeIf { it.value.source == source }
    }

    override suspend fun nameContextNamesBySource(source: String): List<String> =
        nameContexts.values.filter { it.source == source }.map { it.normalizedName }

    override suspend fun deleteNameContextIfSource(normalizedName: String, source: String) {
        val existing = nameContexts[normalizedName] ?: return
        if (existing.source != source) return
        nameContexts.remove(normalizedName)
        nameContextKeywords.remove(normalizedName)
    }

    override suspend fun exactRuleNamesBySource(source: String): List<String> =
        exactRules.values.filter { it.source == source }.map { it.normalizedName }

    override suspend fun deleteExactRuleIfSource(normalizedName: String, source: String) {
        val existing = exactRules[normalizedName] ?: return
        if (existing.source != source) return
        exactRules.remove(normalizedName)
        refreshRememberedCount()
    }

    override suspend fun activeTransitionKeywordIds(): List<Long> =
        transitionKeywords.values.filter { it.active }.map { it.keywordId }

    private fun refreshRememberedCount() {
        userExactRuleCount.value = exactRules.values.count { RememberedRules.counts(it.source) }.toLong()
    }

    override suspend fun insertTransition(entity: CategoryTransitionEntity) {
        transitions += entity
    }

    override suspend fun findTransition(id: String): CategoryTransitionEntity? = transitions.find { it.id == id }

    override suspend fun closeTransition(id: String, closedAt: Long) {
        val index = transitions.indexOfFirst { it.id == id }
        if (index >= 0) {
            transitions[index] = transitions[index].copy(closedAt = closedAt)
        }
    }

    override suspend fun contextCategoryIdsForKeyword(keywordId: Long): List<Long> = nameContextKeywords
        .filter { keywordId in it.value }
        .mapNotNull { nameContexts[it.key]?.categoryId }
        .distinct()

    override suspend fun findActiveByKeyword(keywordId: Long): CategoryTransitionKeywordEntity? =
        transitionKeywords.values.find { it.keywordId == keywordId && it.active }

    override suspend fun countActiveKeywords(transitionId: String): Long =
        transitionKeywords.values.count { it.transitionId == transitionId && it.active }.toLong()

    override suspend fun upsertTransitionKeyword(entity: CategoryTransitionKeywordEntity) {
        transitionKeywords[entity.transitionId to entity.keywordId] = entity
    }

    override suspend fun deactivateTransitionKeyword(transitionId: String, keywordId: Long, deactivatedAt: Long) {
        val key = transitionId to keywordId
        val current = transitionKeywords[key] ?: return
        transitionKeywords[key] = current.copy(active = false, deactivatedAt = deactivatedAt)
    }

    override suspend fun activeTransitionsForKeywords(keywordIds: List<Long>): List<ActiveKeywordTransitionRow> {
        val open = transitions.filter { it.closedAt == null }.associateBy { it.id }
        return transitionKeywords.values.mapNotNull { link ->
            if (!link.active || link.keywordId !in keywordIds) return@mapNotNull null
            val transition = open[link.transitionId] ?: return@mapNotNull null
            ActiveKeywordTransitionRow(link.keywordId, transition.fromCategoryId, transition.toCategoryId)
        }
    }

    override suspend fun getAllExactRules(): List<ExactCategoryRuleEntity> = exactRules.values.toList()

    override suspend fun getAllNameContexts(): List<NameCategoryContextEntity> = nameContexts.values.toList()

    override suspend fun getAllNameContextKeywords(): List<NameCategoryContextKeywordEntity> =
        nameContextKeywords.flatMap { (name, ids) ->
            ids.map { NameCategoryContextKeywordEntity(name, it) }
        }

    override suspend fun getAllExamples(): List<LearningExampleEntity> = examples.values.toList()

    override suspend fun getAllExampleKeywords(): List<LearningExampleKeywordEntity> =
        exampleKeywords.flatMap { (exampleId, ids) ->
            ids.map { LearningExampleKeywordEntity(exampleId, it) }
        }

    override suspend fun getAllTransitions(): List<CategoryTransitionEntity> = transitions.toList()

    override suspend fun getAllTransitionKeywords(): List<CategoryTransitionKeywordEntity> =
        transitionKeywords.values.toList()

    override suspend fun getAllKeywordStats(): List<KeywordCategoryStatEntity> = keywordStats.values.toList()

    override suspend fun getAllLocationStats(): List<LocationCategoryStatEntity> = locationStats.values.toList()
}
