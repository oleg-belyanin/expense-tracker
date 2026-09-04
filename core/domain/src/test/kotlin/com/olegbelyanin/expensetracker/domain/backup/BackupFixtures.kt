package com.olegbelyanin.expensetracker.domain.backup

internal fun sampleBackupSnapshot(
    cafeId: Long = 2,
    expenseId: String = "e-1",
    includeExpense: Boolean = true,
    includeLearning: Boolean = true,
): BackupSnapshot {
    val cafe = BackupCategory(
        id = cafeId,
        code = "CAFE",
        name = "Кафе",
        normalizedName = "кафе",
        color = "#E65100",
        icon = "cafe",
        isBuiltin = true,
        createdAt = 1,
        updatedAt = 1,
    )
    val location = BackupLocation(
        id = 7,
        name = "Шоколадница",
        normalizedName = "шоколадниц",
        usageCount = 1,
        lastUsedAt = 10,
        createdAt = 1,
        updatedAt = 1,
    )
    val keyword = BackupKeyword(id = 3, value = "латт", kind = "word")
    val expense = BackupExpense(
        id = expenseId,
        amountMinor = 15_050,
        spentAt = 1_725_328_000_000,
        name = "Латте",
        normalizedName = "латте",
        categoryId = cafeId,
        locationId = 7,
        comment = "утром",
        categoryAssignmentSource = "explicit",
        dedupKey = "user:e-1",
        createdAt = 1,
        updatedAt = 1,
    )
    val example = BackupLearningExample(
        id = "ex-1",
        expenseId = expenseId,
        normalizedName = "латте",
        categoryId = cafeId,
        proposedCategoryId = cafeId,
        locationId = 7,
        feedbackType = "correction",
        createdAt = 1,
        updatedAt = 1,
        keywordIds = listOf(3),
    )
    return BackupSnapshot(
        schemaVersion = BackupFile.SCHEMA_VERSION,
        normalizerVersion = 1,
        seedDataVersion = 1,
        exportedAtEpochMs = 1_725_328_000_000,
        categories = listOf(cafe),
        locations = listOf(location),
        expenses = if (includeExpense) listOf(expense) else emptyList(),
        keywords = if (includeLearning) listOf(keyword) else emptyList(),
        exactRules = if (includeLearning) {
            listOf(BackupExactRule("латте", cafeId, "correction", createdAt = 1, updatedAt = 1))
        } else {
            emptyList()
        },
        nameContexts = if (includeLearning) {
            listOf(BackupNameContext("латте", cafeId, "correction", updatedAt = 1, keywordIds = listOf(3)))
        } else {
            emptyList()
        },
        learningExamples = if (includeLearning && includeExpense) listOf(example) else emptyList(),
        keywordStats = if (includeLearning) {
            listOf(BackupKeywordStat(3, cafeId, BackupFile.SOURCE_USER, 2))
        } else {
            emptyList()
        },
        locationStats = if (includeLearning) {
            listOf(BackupLocationStat(7, cafeId, BackupFile.SOURCE_USER, 1))
        } else {
            emptyList()
        },
    )
}
