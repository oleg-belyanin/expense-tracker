package com.olegbelyanin.expensetracker.domain.backup

data class LongIdBinding(val backupId: Long, val localId: Long)

data class StringIdBinding(val backupId: String, val localId: String)

data class BackupIdentities(
    val categoryIdByCode: Map<String, Long>,
    val categoryIdByNormalizedName: Map<String, Long>,
    val categoriesById: Map<Long, BackupCategory>,
    val locationIdByNormalizedName: Map<String, Long>,
    val locationsById: Map<Long, BackupLocation>,
    val keywordIdByKindValue: Map<String, Long>,
    val keywordsById: Map<Long, BackupKeyword>,
    val expenseIdById: Map<String, String>,
    val expenseIdByDedupKey: Map<String, String>,
    val exactRuleSourceByName: Map<String, String>,
    val nameContextSourceByName: Map<String, String>,
    val exampleIds: Set<String>,
    val exampleExpenseIds: Set<String>,
    val transitionIds: Set<String>,
    val keywordStatKeys: Set<String>,
    val locationStatKeys: Set<String>,
) {
    companion object {
        fun from(snapshot: BackupSnapshot): BackupIdentities {
            val categoriesById = snapshot.categories.associateBy { it.id }
            val locationsById = snapshot.locations.associateBy { it.id }
            val keywordsById = snapshot.keywords.associateBy { it.id }
            return BackupIdentities(
                categoryIdByCode = snapshot.categories.mapNotNull { category ->
                    category.code?.let { it to category.id }
                }.toMap(),
                categoryIdByNormalizedName = snapshot.categories.associate { it.normalizedName to it.id },
                categoriesById = categoriesById,
                locationIdByNormalizedName = snapshot.locations.associate { it.normalizedName to it.id },
                locationsById = locationsById,
                keywordIdByKindValue = snapshot.keywords.associate { keywordKey(it.kind, it.value) to it.id },
                keywordsById = keywordsById,
                expenseIdById = snapshot.expenses.associate { it.id to it.id },
                expenseIdByDedupKey = snapshot.expenses.associate { it.dedupKey to it.id },
                exactRuleSourceByName = snapshot.exactRules.associate { it.normalizedName to it.source },
                nameContextSourceByName = snapshot.nameContexts.associate { it.normalizedName to it.source },
                exampleIds = snapshot.learningExamples.map { it.id }.toSet(),
                exampleExpenseIds = snapshot.learningExamples.mapNotNull { it.expenseId }.toSet(),
                transitionIds = snapshot.transitions.map { it.id }.toSet(),
                keywordStatKeys = snapshot.keywordStats.mapNotNull { stat ->
                    val keyword = keywordsById[stat.keywordId] ?: return@mapNotNull null
                    val category = categoriesById[stat.categoryId] ?: return@mapNotNull null
                    keywordStatKey(keyword, category, stat.source)
                }.toSet(),
                locationStatKeys = snapshot.locationStats.mapNotNull { stat ->
                    val location = locationsById[stat.locationId] ?: return@mapNotNull null
                    val category = categoriesById[stat.categoryId] ?: return@mapNotNull null
                    locationStatKey(location, category, stat.source)
                }.toSet(),
            )
        }

        fun keywordKey(kind: String, value: String): String = "$kind\u0000$value"

        fun categoryKey(category: BackupCategory): String {
            val code = category.code
            return if (category.isBuiltin && !code.isNullOrBlank()) "code:$code" else "name:${category.normalizedName}"
        }

        fun keywordStatKey(keyword: BackupKeyword, category: BackupCategory, source: String): String =
            "${keywordKey(keyword.kind, keyword.value)}\u0000${categoryKey(category)}\u0000$source"

        fun locationStatKey(location: BackupLocation, category: BackupCategory, source: String): String =
            "${location.normalizedName}\u0000${categoryKey(category)}\u0000$source"
    }
}

data class BackupRestorePlan(
    val categoryBindings: List<LongIdBinding>,
    val categoriesToInsert: List<BackupCategory>,
    val locationBindings: List<LongIdBinding>,
    val locationsToInsert: List<BackupLocation>,
    val keywordBindings: List<LongIdBinding>,
    val keywordsToInsert: List<BackupKeyword>,
    val expenseBindings: List<StringIdBinding>,
    val expensesToInsert: List<BackupExpense>,
    val expensesSkipped: Int,
    val exactRulesToInsert: List<BackupExactRule>,
    val nameContextsToInsert: List<BackupNameContext>,
    val examplesToInsert: List<BackupLearningExample>,
    val examplesSkipped: Int,
    val transitionsToInsert: List<BackupTransition>,
    val keywordStatsToInsert: List<BackupKeywordStat>,
    val locationStatsToInsert: List<BackupLocationStat>,
) {
    fun toResult(): BackupRestoreResult = BackupRestoreResult(
        expensesInserted = expensesToInsert.size,
        expensesSkipped = expensesSkipped,
        examplesInserted = examplesToInsert.size,
        examplesSkipped = examplesSkipped,
        categoriesInserted = categoriesToInsert.size,
        locationsInserted = locationsToInsert.size,
        rulesInserted = exactRulesToInsert.size,
    )
}

object BackupRestorePlanner {
    fun plan(current: BackupIdentities, incoming: BackupSnapshot): BackupRestorePlan {
        val incomingCategories = incoming.categories.associateBy { it.id }
        val incomingLocations = incoming.locations.associateBy { it.id }
        val incomingKeywords = incoming.keywords.associateBy { it.id }

        val categoryBindings = mutableListOf<LongIdBinding>()
        val categoriesToInsert = mutableListOf<BackupCategory>()
        incoming.categories.forEach { category ->
            val localId = resolveCategory(current, category)
            if (localId != null) {
                categoryBindings += LongIdBinding(category.id, localId)
            } else if (category.isBuiltin) {
                throw BackupIncompatibleException()
            } else {
                categoriesToInsert += category
            }
        }

        val locationBindings = mutableListOf<LongIdBinding>()
        val locationsToInsert = mutableListOf<BackupLocation>()
        incoming.locations.forEach { location ->
            val localId = current.locationIdByNormalizedName[location.normalizedName]
            if (localId != null) {
                locationBindings += LongIdBinding(location.id, localId)
            } else {
                locationsToInsert += location
            }
        }

        val keywordBindings = mutableListOf<LongIdBinding>()
        val keywordsToInsert = mutableListOf<BackupKeyword>()
        incoming.keywords.forEach { keyword ->
            val localId = current.keywordIdByKindValue[BackupIdentities.keywordKey(keyword.kind, keyword.value)]
            if (localId != null) {
                keywordBindings += LongIdBinding(keyword.id, localId)
            } else {
                keywordsToInsert += keyword
            }
        }

        val expenseBindings = mutableListOf<StringIdBinding>()
        val expensesToInsert = mutableListOf<BackupExpense>()
        var expensesSkipped = 0
        incoming.expenses.forEach { expense ->
            val existingId = current.expenseIdById[expense.id] ?: current.expenseIdByDedupKey[expense.dedupKey]
            if (existingId != null) {
                expenseBindings += StringIdBinding(expense.id, existingId)
                expensesSkipped++
            } else {
                expensesToInsert += expense
                expenseBindings += StringIdBinding(expense.id, expense.id)
            }
        }

        val exactRulesToInsert = incoming.exactRules.filter { rule ->
            shouldReplaceUserLearning(current.exactRuleSourceByName[rule.normalizedName])
        }
        val nameContextsToInsert = incoming.nameContexts.filter { context ->
            shouldReplaceUserLearning(current.nameContextSourceByName[context.normalizedName])
        }

        val examplesToInsert = mutableListOf<BackupLearningExample>()
        var examplesSkipped = 0
        incoming.learningExamples.forEach { example ->
            val localExpenseId = example.expenseId?.let { backupId ->
                expenseBindings.find { it.backupId == backupId }?.localId
            }
            val duplicate = example.id in current.exampleIds ||
                (localExpenseId != null && localExpenseId in current.exampleExpenseIds)
            if (duplicate) {
                examplesSkipped++
            } else {
                examplesToInsert += example
            }
        }

        val transitionsToInsert = incoming.transitions.filter { it.id !in current.transitionIds }

        val keywordStatsToInsert = incoming.keywordStats.filter { stat ->
            val keyword = incomingKeywords[stat.keywordId]
            val category = incomingCategories[stat.categoryId]
            if (keyword == null || category == null) return@filter false
            BackupIdentities.keywordStatKey(keyword, category, stat.source) !in current.keywordStatKeys
        }
        val locationStatsToInsert = incoming.locationStats.filter { stat ->
            val location = incomingLocations[stat.locationId]
            val category = incomingCategories[stat.categoryId]
            if (location == null || category == null) return@filter false
            BackupIdentities.locationStatKey(location, category, stat.source) !in current.locationStatKeys
        }

        return BackupRestorePlan(
            categoryBindings = categoryBindings,
            categoriesToInsert = categoriesToInsert,
            locationBindings = locationBindings,
            locationsToInsert = locationsToInsert,
            keywordBindings = keywordBindings,
            keywordsToInsert = keywordsToInsert,
            expenseBindings = expenseBindings,
            expensesToInsert = expensesToInsert,
            expensesSkipped = expensesSkipped,
            exactRulesToInsert = exactRulesToInsert,
            nameContextsToInsert = nameContextsToInsert,
            examplesToInsert = examplesToInsert,
            examplesSkipped = examplesSkipped,
            transitionsToInsert = transitionsToInsert,
            keywordStatsToInsert = keywordStatsToInsert,
            locationStatsToInsert = locationStatsToInsert,
        )
    }

    private fun resolveCategory(current: BackupIdentities, category: BackupCategory): Long? {
        val byCode = category.code?.let { current.categoryIdByCode[it] }
        if (category.isBuiltin && byCode != null) return byCode
        return current.categoryIdByNormalizedName[category.normalizedName]
    }

    private fun shouldReplaceUserLearning(existingSource: String?): Boolean =
        existingSource == null || existingSource == BackupFile.SOURCE_SEED
}
