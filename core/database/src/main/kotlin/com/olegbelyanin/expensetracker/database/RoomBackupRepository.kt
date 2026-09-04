package com.olegbelyanin.expensetracker.database

import androidx.room3.withWriteTransaction
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.entities.CategoryEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.ExactCategoryRuleEntity
import com.olegbelyanin.expensetracker.database.entities.ExpenseEntity
import com.olegbelyanin.expensetracker.database.entities.ExpenseFtsEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LocationCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.LocationEntity
import com.olegbelyanin.expensetracker.database.entities.LocationFtsEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextKeywordEntity
import com.olegbelyanin.expensetracker.database.learning.CategoryNameExperienceWriter
import com.olegbelyanin.expensetracker.database.seed.SeedImporter
import com.olegbelyanin.expensetracker.domain.BackupRepository
import com.olegbelyanin.expensetracker.domain.backup.BackupCategory
import com.olegbelyanin.expensetracker.domain.backup.BackupExactRule
import com.olegbelyanin.expensetracker.domain.backup.BackupExpense
import com.olegbelyanin.expensetracker.domain.backup.BackupFile
import com.olegbelyanin.expensetracker.domain.backup.BackupIdentities
import com.olegbelyanin.expensetracker.domain.backup.BackupKeyword
import com.olegbelyanin.expensetracker.domain.backup.BackupKeywordStat
import com.olegbelyanin.expensetracker.domain.backup.BackupLearningExample
import com.olegbelyanin.expensetracker.domain.backup.BackupLocation
import com.olegbelyanin.expensetracker.domain.backup.BackupLocationStat
import com.olegbelyanin.expensetracker.domain.backup.BackupNameContext
import com.olegbelyanin.expensetracker.domain.backup.BackupRestorePlan
import com.olegbelyanin.expensetracker.domain.backup.BackupRestoreResult
import com.olegbelyanin.expensetracker.domain.backup.BackupSnapshot
import com.olegbelyanin.expensetracker.domain.backup.BackupTransition
import com.olegbelyanin.expensetracker.domain.backup.BackupTransitionKeyword
import java.time.Clock

class RoomBackupRepository(
    private val database: AppDatabase,
    private val normalizer: TextNormalizer,
    private val clock: Clock,
) : BackupRepository {
    private val nameExperience = CategoryNameExperienceWriter(database, normalizer)

    override suspend fun exportSnapshot(): BackupSnapshot = readSnapshot(includeSeedLearning = false)

    override suspend fun identities(): BackupIdentities =
        BackupIdentities.from(readSnapshot(includeSeedLearning = true))

    override suspend fun apply(plan: BackupRestorePlan): BackupRestoreResult = database.withWriteTransaction {
        val maps = IdMaps()
        plan.categoryBindings.forEach { maps.categories[it.backupId] = it.localId }
        plan.locationBindings.forEach { maps.locations[it.backupId] = it.localId }
        plan.keywordBindings.forEach { maps.keywords[it.backupId] = it.localId }
        plan.expenseBindings.forEach { maps.expenses[it.backupId] = it.localId }

        plan.categoriesToInsert.forEach { category ->
            val existing = database.categoryDao().findByNormalizedName(category.normalizedName)
            val localId = existing?.id ?: insertUserCategory(category)
            maps.categories[category.id] = localId
            if (existing == null) {
                nameExperience.writeIfMissing(localId, category.name)
            }
        }
        plan.locationsToInsert.forEach { location ->
            val existing = database.locationDao().findByNormalizedName(location.normalizedName)
            val localId = existing?.id ?: insertLocation(location)
            maps.locations[location.id] = localId
        }
        plan.keywordsToInsert.forEach { keyword ->
            val existing = database.keywordDao().find(keyword.kind, keyword.value)
            val localId = existing?.id ?: database.keywordDao().insert(
                KeywordEntity(value = keyword.value, kind = keyword.kind),
            )
            maps.keywords[keyword.id] = localId
        }
        plan.expensesToInsert.forEach { expense ->
            insertExpense(expense, maps)
            maps.expenses[expense.id] = expense.id
        }
        plan.exactRulesToInsert.forEach { rule ->
            val existing = database.learningDao().findExactRule(rule.normalizedName)
            if (existing != null && existing.source != BackupFile.SOURCE_SEED) return@forEach
            database.learningDao().upsertExactRule(
                ExactCategoryRuleEntity(
                    normalizedName = rule.normalizedName,
                    categoryId = maps.category(rule.categoryId),
                    source = rule.source,
                    createdAt = existing?.createdAt ?: rule.createdAt,
                    updatedAt = rule.updatedAt,
                ),
            )
        }
        plan.nameContextsToInsert.forEach { context ->
            val existing = database.learningDao().findNameContext(context.normalizedName)
            if (existing != null && existing.source != BackupFile.SOURCE_SEED) return@forEach
            database.learningDao().upsertNameContext(
                NameCategoryContextEntity(
                    normalizedName = context.normalizedName,
                    categoryId = maps.category(context.categoryId),
                    source = context.source,
                    updatedAt = context.updatedAt,
                ),
            )
            database.learningDao().deleteNameContextKeywords(context.normalizedName)
            context.keywordIds.distinct().forEach { keywordId ->
                database.learningDao().insertNameContextKeyword(
                    NameCategoryContextKeywordEntity(context.normalizedName, maps.keyword(keywordId)),
                )
            }
        }
        plan.examplesToInsert.forEach { example ->
            if (database.learningDao().findExampleById(example.id) != null) return@forEach
            val localExpenseId = maps.expense(example.expenseId)
            if (localExpenseId != null && database.learningDao().findExampleByExpenseId(localExpenseId) != null) {
                return@forEach
            }
            database.learningDao().upsertExample(
                LearningExampleEntity(
                    id = example.id,
                    expenseId = localExpenseId,
                    normalizedName = example.normalizedName,
                    categoryId = maps.category(example.categoryId),
                    proposedCategoryId = example.proposedCategoryId?.let(maps::category),
                    locationId = maps.location(example.locationId),
                    feedbackType = example.feedbackType,
                    createdAt = example.createdAt,
                    updatedAt = example.updatedAt,
                ),
            )
            database.learningDao().deleteExampleKeywords(example.id)
            example.keywordIds.distinct().forEach { keywordId ->
                database.learningDao().insertExampleKeyword(
                    LearningExampleKeywordEntity(example.id, maps.keyword(keywordId)),
                )
            }
        }
        plan.transitionsToInsert.forEach { transition ->
            if (database.learningDao().findTransition(transition.id) != null) return@forEach
            database.learningDao().insertTransition(
                CategoryTransitionEntity(
                    id = transition.id,
                    fromCategoryId = maps.category(transition.fromCategoryId),
                    toCategoryId = maps.category(transition.toCategoryId),
                    createdAt = transition.createdAt,
                    closedAt = transition.closedAt,
                ),
            )
            transition.keywords.forEach { keyword ->
                database.learningDao().upsertTransitionKeyword(
                    CategoryTransitionKeywordEntity(
                        transitionId = transition.id,
                        keywordId = maps.keyword(keyword.keywordId),
                        active = keyword.active,
                        deactivatedAt = keyword.deactivatedAt,
                    ),
                )
            }
        }
        plan.keywordStatsToInsert.forEach { stat ->
            val keywordId = maps.keyword(stat.keywordId)
            val categoryId = maps.category(stat.categoryId)
            if (database.learningDao().findKeywordStat(keywordId, categoryId, stat.source) != null) {
                return@forEach
            }
            database.learningDao().upsertKeywordStat(
                KeywordCategoryStatEntity(keywordId, categoryId, stat.source, stat.observationCount),
            )
        }
        plan.locationStatsToInsert.forEach { stat ->
            val locationId = maps.location(stat.locationId) ?: return@forEach
            val categoryId = maps.category(stat.categoryId)
            if (database.learningDao().findLocationStat(locationId, categoryId, stat.source) != null) {
                return@forEach
            }
            database.learningDao().upsertLocationStat(
                LocationCategoryStatEntity(locationId, categoryId, stat.source, stat.observationCount),
            )
        }
        plan.toResult()
    }

    private suspend fun readSnapshot(includeSeedLearning: Boolean): BackupSnapshot {
        val categories = database.categoryDao().getAll().map { it.toBackup() }
        val locations = database.locationDao().getAll().map { it.toBackup() }
        val expenses = database.expenseDao().getAll().map { it.toBackup() }
        val allKeywords = database.keywordDao().getAll()
        val exactRules = database.learningDao().getAllExactRules()
            .filter { includeSeedLearning || it.source != BackupFile.SOURCE_SEED }
            .map { it.toBackup() }
        val nameContextKeywords = database.learningDao().getAllNameContextKeywords()
            .groupBy { it.normalizedName }
            .mapValues { (_, rows) -> rows.map { it.keywordId } }
        val nameContexts = database.learningDao().getAllNameContexts()
            .filter { includeSeedLearning || it.source != BackupFile.SOURCE_SEED }
            .map { it.toBackup(nameContextKeywords[it.normalizedName].orEmpty()) }
        val exampleKeywords = database.learningDao().getAllExampleKeywords()
            .groupBy { it.learningExampleId }
            .mapValues { (_, rows) -> rows.map { it.keywordId } }
        val examples = database.learningDao().getAllExamples()
            .map { it.toBackup(exampleKeywords[it.id].orEmpty()) }
        val transitionKeywords = database.learningDao().getAllTransitionKeywords().groupBy { it.transitionId }
        val transitions = database.learningDao().getAllTransitions().map { transition ->
            transition.toBackup(
                transitionKeywords[transition.id].orEmpty().map { link ->
                    BackupTransitionKeyword(link.keywordId, link.active, link.deactivatedAt)
                },
            )
        }
        val keywordStats = database.learningDao().getAllKeywordStats()
            .filter { it.source == BackupFile.SOURCE_USER }
            .map { it.toBackup() }
        val locationStats = database.learningDao().getAllLocationStats()
            .filter { it.source == BackupFile.SOURCE_USER }
            .map { it.toBackup() }
        val usedKeywordIds = buildSet {
            nameContexts.forEach { addAll(it.keywordIds) }
            examples.forEach { addAll(it.keywordIds) }
            transitions.forEach { transition -> transition.keywords.forEach { add(it.keywordId) } }
            keywordStats.forEach { add(it.keywordId) }
        }
        val keywords = allKeywords
            .filter { includeSeedLearning || it.id in usedKeywordIds }
            .map { it.toBackup() }
        return BackupSnapshot(
            schemaVersion = AppDatabase.SCHEMA_VERSION,
            normalizerVersion = database.metaDao().get(SeedImporter.NORMALIZER_VERSION_KEY)?.toIntOrNull()
                ?: TextNormalizer.VERSION,
            seedDataVersion = database.metaDao().get(SeedImporter.SEED_DATA_VERSION_KEY)?.toIntOrNull() ?: 1,
            exportedAtEpochMs = clock.millis(),
            categories = categories,
            locations = locations,
            expenses = expenses,
            keywords = keywords,
            exactRules = exactRules,
            nameContexts = nameContexts,
            learningExamples = examples,
            transitions = transitions,
            keywordStats = keywordStats,
            locationStats = locationStats,
        )
    }

    private suspend fun insertUserCategory(category: BackupCategory): Long = database.categoryDao().insert(
        CategoryEntity(
            code = null,
            name = category.name,
            normalizedName = category.normalizedName,
            color = category.color,
            icon = category.icon,
            isBuiltin = false,
            archivedAt = category.archivedAt,
            createdAt = category.createdAt,
            updatedAt = category.updatedAt,
        ),
    )

    private suspend fun insertLocation(location: BackupLocation): Long {
        val id = database.locationDao().insert(
            LocationEntity(
                name = location.name,
                normalizedName = location.normalizedName,
                usageCount = location.usageCount,
                lastUsedAt = location.lastUsedAt,
                archivedAt = location.archivedAt,
                createdAt = location.createdAt,
                updatedAt = location.updatedAt,
            ),
        )
        syncLocationFts(id, location.name, location.normalizedName)
        return id
    }

    private suspend fun insertExpense(expense: BackupExpense, maps: IdMaps) {
        val entity = ExpenseEntity(
            id = expense.id,
            amountMinor = expense.amountMinor,
            spentAt = expense.spentAt,
            name = expense.name,
            normalizedName = expense.normalizedName,
            categoryId = maps.category(expense.categoryId),
            locationId = maps.location(expense.locationId),
            comment = expense.comment,
            categoryAssignmentSource = expense.categoryAssignmentSource,
            dedupKey = expense.dedupKey,
            createdAt = expense.createdAt,
            updatedAt = expense.updatedAt,
        )
        database.expenseDao().insert(entity)
        syncExpenseFts(entity)
    }

    private suspend fun syncExpenseFts(entity: ExpenseEntity) {
        val rowId = database.expenseDao().rowIdById(entity.id) ?: return
        val ftsRowId = rowId.toInt()
        database.expenseFtsDao().deleteByRowId(ftsRowId)
        database.expenseFtsDao().upsert(
            ExpenseFtsEntity(
                rowid = ftsRowId,
                name = entity.name,
                normalizedName = entity.normalizedName,
                comment = entity.comment.orEmpty(),
            ),
        )
    }

    private suspend fun syncLocationFts(id: Long, name: String, normalizedName: String) {
        val rowId = id.toInt()
        database.locationFtsDao().deleteByRowId(rowId)
        database.locationFtsDao().upsert(
            LocationFtsEntity(rowid = rowId, name = name, normalizedName = normalizedName),
        )
    }

    private class IdMaps {
        val categories = mutableMapOf<Long, Long>()
        val locations = mutableMapOf<Long, Long>()
        val keywords = mutableMapOf<Long, Long>()
        val expenses = mutableMapOf<String, String>()

        fun category(backupId: Long): Long = categories.getValue(backupId)

        fun location(backupId: Long?): Long? = backupId?.let { locations.getValue(it) }

        fun keyword(backupId: Long): Long = keywords.getValue(backupId)

        fun expense(backupId: String?): String? = backupId?.let { expenses.getValue(it) }
    }
}

private fun CategoryEntity.toBackup() = BackupCategory(
    id = id,
    code = code,
    name = name,
    normalizedName = normalizedName,
    color = color,
    icon = icon,
    isBuiltin = isBuiltin,
    archivedAt = archivedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun LocationEntity.toBackup() = BackupLocation(
    id = id,
    name = name,
    normalizedName = normalizedName,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt,
    archivedAt = archivedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun ExpenseEntity.toBackup() = BackupExpense(
    id = id,
    amountMinor = amountMinor,
    spentAt = spentAt,
    name = name,
    normalizedName = normalizedName,
    categoryId = categoryId,
    locationId = locationId,
    comment = comment,
    categoryAssignmentSource = categoryAssignmentSource,
    dedupKey = dedupKey,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun KeywordEntity.toBackup() = BackupKeyword(id = id, value = value, kind = kind)

private fun ExactCategoryRuleEntity.toBackup() = BackupExactRule(
    normalizedName = normalizedName,
    categoryId = categoryId,
    source = source,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun NameCategoryContextEntity.toBackup(keywordIds: List<Long>) = BackupNameContext(
    normalizedName = normalizedName,
    categoryId = categoryId,
    source = source,
    updatedAt = updatedAt,
    keywordIds = keywordIds,
)

private fun LearningExampleEntity.toBackup(keywordIds: List<Long>) = BackupLearningExample(
    id = id,
    expenseId = expenseId,
    normalizedName = normalizedName,
    categoryId = categoryId,
    proposedCategoryId = proposedCategoryId,
    locationId = locationId,
    feedbackType = feedbackType,
    createdAt = createdAt,
    updatedAt = updatedAt,
    keywordIds = keywordIds,
)

private fun CategoryTransitionEntity.toBackup(keywords: List<BackupTransitionKeyword>) = BackupTransition(
    id = id,
    fromCategoryId = fromCategoryId,
    toCategoryId = toCategoryId,
    createdAt = createdAt,
    closedAt = closedAt,
    keywords = keywords,
)

private fun KeywordCategoryStatEntity.toBackup() = BackupKeywordStat(
    keywordId = keywordId,
    categoryId = categoryId,
    source = source,
    observationCount = observationCount,
)

private fun LocationCategoryStatEntity.toBackup() = BackupLocationStat(
    locationId = locationId,
    categoryId = categoryId,
    source = source,
    observationCount = observationCount,
)
