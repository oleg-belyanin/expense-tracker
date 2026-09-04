package com.olegbelyanin.expensetracker.domain.backup

import com.olegbelyanin.expensetracker.domain.BackupRepository

class InMemoryBackupRepository(initial: BackupSnapshot = BackupSnapshot()) : BackupRepository {
    var snapshot: BackupSnapshot = initial
        private set
    val applyCalls = mutableListOf<BackupRestorePlan>()

    override suspend fun exportSnapshot(): BackupSnapshot = snapshot

    override suspend fun identities(): BackupIdentities = BackupIdentities.from(snapshot)

    override suspend fun apply(plan: BackupRestorePlan): BackupRestoreResult {
        applyCalls += plan
        val categories = snapshot.categories.associateBy { it.id }.toMutableMap()
        plan.categoriesToInsert.forEach { categories[it.id] = it }
        val locations = snapshot.locations.associateBy { it.id }.toMutableMap()
        plan.locationsToInsert.forEach { locations[it.id] = it }
        val keywords = snapshot.keywords.associateBy { it.id }.toMutableMap()
        plan.keywordsToInsert.forEach { keywords[it.id] = it }
        val expenses = snapshot.expenses.associateBy { it.id }.toMutableMap()
        plan.expensesToInsert.forEach { expenses[it.id] = it }
        val exactRules = snapshot.exactRules.associateBy { it.normalizedName }.toMutableMap()
        plan.exactRulesToInsert.forEach { exactRules[it.normalizedName] = it }
        val nameContexts = snapshot.nameContexts.associateBy { it.normalizedName }.toMutableMap()
        plan.nameContextsToInsert.forEach { nameContexts[it.normalizedName] = it }
        val examples = snapshot.learningExamples.associateBy { it.id }.toMutableMap()
        plan.examplesToInsert.forEach { examples[it.id] = it }
        val transitions = snapshot.transitions.associateBy { it.id }.toMutableMap()
        plan.transitionsToInsert.forEach { transitions[it.id] = it }
        val keywordStats = snapshot.keywordStats.associateBy {
            Triple(it.keywordId, it.categoryId, it.source)
        }.toMutableMap()
        plan.keywordStatsToInsert.forEach { keywordStats[Triple(it.keywordId, it.categoryId, it.source)] = it }
        val locationStats = snapshot.locationStats.associateBy {
            Triple(it.locationId, it.categoryId, it.source)
        }.toMutableMap()
        plan.locationStatsToInsert.forEach { locationStats[Triple(it.locationId, it.categoryId, it.source)] = it }
        snapshot = snapshot.copy(
            categories = categories.values.toList(),
            locations = locations.values.toList(),
            keywords = keywords.values.toList(),
            expenses = expenses.values.toList(),
            exactRules = exactRules.values.toList(),
            nameContexts = nameContexts.values.toList(),
            learningExamples = examples.values.toList(),
            transitions = transitions.values.toList(),
            keywordStats = keywordStats.values.toList(),
            locationStats = locationStats.values.toList(),
        )
        return plan.toResult()
    }
}
