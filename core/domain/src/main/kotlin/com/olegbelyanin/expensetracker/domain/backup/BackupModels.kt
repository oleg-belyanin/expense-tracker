package com.olegbelyanin.expensetracker.domain.backup

import kotlinx.serialization.Serializable

open class BackupException(val userMessage: String) : IllegalArgumentException(userMessage)

class BackupCorruptedException(userMessage: String = DEFAULT_MESSAGE) : BackupException(userMessage) {
    companion object {
        const val DEFAULT_MESSAGE =
            "Файл повреждён или имеет чужой формат. Существующие данные не изменены."
    }
}

class BackupIncompatibleException(userMessage: String = DEFAULT_MESSAGE) : BackupException(userMessage) {
    companion object {
        const val DEFAULT_MESSAGE =
            "Эта копия не подходит к текущей версии приложения. Существующие данные не изменены."
    }
}

data class BackupRestoreResult(
    val expensesInserted: Int,
    val expensesSkipped: Int,
    val examplesInserted: Int,
    val examplesSkipped: Int,
    val categoriesInserted: Int,
    val locationsInserted: Int,
    val rulesInserted: Int,
)

@Serializable
data class BackupFile(
    val format: String,
    val formatVersion: Int,
    val schemaVersion: Int,
    val normalizerVersion: Int,
    val seedDataVersion: Int,
    val exportedAt: String,
    val categories: List<BackupCategory> = emptyList(),
    val locations: List<BackupLocation> = emptyList(),
    val expenses: List<BackupExpense> = emptyList(),
    val keywords: List<BackupKeyword> = emptyList(),
    val exactRules: List<BackupExactRule> = emptyList(),
    val nameContexts: List<BackupNameContext> = emptyList(),
    val learningExamples: List<BackupLearningExample> = emptyList(),
    val transitions: List<BackupTransition> = emptyList(),
    val keywordStats: List<BackupKeywordStat> = emptyList(),
    val locationStats: List<BackupLocationStat> = emptyList(),
) {
    fun toSnapshot(exportedAtEpochMs: Long): BackupSnapshot = BackupSnapshot(
        schemaVersion = schemaVersion,
        normalizerVersion = normalizerVersion,
        seedDataVersion = seedDataVersion,
        exportedAtEpochMs = exportedAtEpochMs,
        categories = categories,
        locations = locations,
        expenses = expenses,
        keywords = keywords,
        exactRules = exactRules,
        nameContexts = nameContexts,
        learningExamples = learningExamples,
        transitions = transitions,
        keywordStats = keywordStats,
        locationStats = locationStats,
    )

    companion object {
        const val FORMAT = "expense-tracker-backup"
        const val FORMAT_VERSION = 1
        const val SCHEMA_VERSION = 1
        const val SOURCE_SEED = "seed"
        const val SOURCE_USER = "user"
    }
}

data class BackupSnapshot(
    val schemaVersion: Int = BackupFile.SCHEMA_VERSION,
    val normalizerVersion: Int = 1,
    val seedDataVersion: Int = 1,
    val exportedAtEpochMs: Long = 0,
    val categories: List<BackupCategory> = emptyList(),
    val locations: List<BackupLocation> = emptyList(),
    val expenses: List<BackupExpense> = emptyList(),
    val keywords: List<BackupKeyword> = emptyList(),
    val exactRules: List<BackupExactRule> = emptyList(),
    val nameContexts: List<BackupNameContext> = emptyList(),
    val learningExamples: List<BackupLearningExample> = emptyList(),
    val transitions: List<BackupTransition> = emptyList(),
    val keywordStats: List<BackupKeywordStat> = emptyList(),
    val locationStats: List<BackupLocationStat> = emptyList(),
)

@Serializable
data class BackupCategory(
    val id: Long,
    val code: String? = null,
    val name: String,
    val normalizedName: String,
    val color: String,
    val icon: String,
    val isBuiltin: Boolean,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupLocation(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupExpense(
    val id: String,
    val amountMinor: Long,
    val spentAt: Long,
    val name: String,
    val normalizedName: String,
    val categoryId: Long,
    val locationId: Long? = null,
    val comment: String? = null,
    val categoryAssignmentSource: String,
    val dedupKey: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupKeyword(val id: Long, val value: String, val kind: String)

@Serializable
data class BackupExactRule(
    val normalizedName: String,
    val categoryId: Long,
    val source: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupNameContext(
    val normalizedName: String,
    val categoryId: Long,
    val source: String,
    val updatedAt: Long,
    val keywordIds: List<Long> = emptyList(),
)

@Serializable
data class BackupLearningExample(
    val id: String,
    val expenseId: String? = null,
    val normalizedName: String,
    val categoryId: Long,
    val proposedCategoryId: Long? = null,
    val locationId: Long? = null,
    val feedbackType: String,
    val createdAt: Long,
    val updatedAt: Long,
    val keywordIds: List<Long> = emptyList(),
)

@Serializable
data class BackupTransition(
    val id: String,
    val fromCategoryId: Long,
    val toCategoryId: Long,
    val createdAt: Long,
    val closedAt: Long? = null,
    val keywords: List<BackupTransitionKeyword> = emptyList(),
)

@Serializable
data class BackupTransitionKeyword(val keywordId: Long, val active: Boolean, val deactivatedAt: Long? = null)

@Serializable
data class BackupKeywordStat(val keywordId: Long, val categoryId: Long, val source: String, val observationCount: Int)

@Serializable
data class BackupLocationStat(
    val locationId: Long,
    val categoryId: Long,
    val source: String,
    val observationCount: Int,
)
