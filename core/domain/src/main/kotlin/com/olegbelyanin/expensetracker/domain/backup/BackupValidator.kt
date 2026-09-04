package com.olegbelyanin.expensetracker.domain.backup

import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource

object BackupValidator {
    fun validate(file: BackupFile) {
        if (file.format != BackupFile.FORMAT || file.formatVersion != BackupFile.FORMAT_VERSION) {
            throw BackupCorruptedException()
        }
        if (file.schemaVersion != BackupFile.SCHEMA_VERSION) {
            throw BackupIncompatibleException()
        }
        val categoryIds = uniqueIds(file.categories.map { it.id })
        val locationIds = uniqueIds(file.locations.map { it.id })
        val keywordIds = uniqueIds(file.keywords.map { it.id })
        uniqueStrings(file.expenses.map { it.id })
        uniqueStrings(file.expenses.map { it.dedupKey })
        uniqueStrings(file.learningExamples.map { it.id })
        uniqueStrings(file.transitions.map { it.id })
        file.categories.forEach { category ->
            if (category.name.isBlank() || category.normalizedName.isBlank()) {
                throw BackupCorruptedException()
            }
            if (category.isBuiltin && category.code.isNullOrBlank()) {
                throw BackupCorruptedException()
            }
        }
        file.locations.forEach { location ->
            if (location.name.isBlank() || location.normalizedName.isBlank()) {
                throw BackupCorruptedException()
            }
        }
        file.keywords.forEach { keyword ->
            if (keyword.value.isBlank() || keyword.kind.isBlank()) {
                throw BackupCorruptedException()
            }
        }
        file.expenses.forEach { expense ->
            if (expense.id.isBlank() || expense.name.isBlank() || expense.dedupKey.isBlank()) {
                throw BackupCorruptedException()
            }
            if (expense.amountMinor <= 0) throw BackupCorruptedException()
            if (expense.categoryId !in categoryIds) throw BackupCorruptedException()
            if (expense.locationId != null && expense.locationId !in locationIds) {
                throw BackupCorruptedException()
            }
            parseAssignmentSource(expense.categoryAssignmentSource)
        }
        file.exactRules.forEach { rule ->
            if (rule.normalizedName.isBlank() || rule.categoryId !in categoryIds) {
                throw BackupCorruptedException()
            }
        }
        file.nameContexts.forEach { context ->
            if (context.normalizedName.isBlank() || context.categoryId !in categoryIds) {
                throw BackupCorruptedException()
            }
            requireKeywords(context.keywordIds, keywordIds)
        }
        file.learningExamples.forEach { example ->
            if (example.id.isBlank() || example.normalizedName.isBlank()) {
                throw BackupCorruptedException()
            }
            if (example.categoryId !in categoryIds) throw BackupCorruptedException()
            if (example.proposedCategoryId != null && example.proposedCategoryId !in categoryIds) {
                throw BackupCorruptedException()
            }
            if (example.locationId != null && example.locationId !in locationIds) {
                throw BackupCorruptedException()
            }
            if (example.expenseId != null && file.expenses.none { it.id == example.expenseId }) {
                throw BackupCorruptedException()
            }
            requireKeywords(example.keywordIds, keywordIds)
        }
        file.transitions.forEach { transition ->
            if (transition.id.isBlank()) throw BackupCorruptedException()
            if (transition.fromCategoryId !in categoryIds || transition.toCategoryId !in categoryIds) {
                throw BackupCorruptedException()
            }
            requireKeywords(transition.keywords.map { it.keywordId }, keywordIds)
        }
        file.keywordStats.forEach { stat ->
            if (stat.keywordId !in keywordIds || stat.categoryId !in categoryIds) {
                throw BackupCorruptedException()
            }
            if (stat.observationCount <= 0) throw BackupCorruptedException()
        }
        file.locationStats.forEach { stat ->
            if (stat.locationId !in locationIds || stat.categoryId !in categoryIds) {
                throw BackupCorruptedException()
            }
            if (stat.observationCount <= 0) throw BackupCorruptedException()
        }
    }

    fun validateAgainstDevice(file: BackupFile, identities: BackupIdentities) {
        file.categories.filter { it.isBuiltin }.forEach { category ->
            val code = category.code ?: throw BackupCorruptedException()
            if (identities.categoryIdByCode[code] == null) {
                throw BackupIncompatibleException()
            }
        }
    }

    private fun uniqueIds(ids: List<Long>): Set<Long> {
        val unique = ids.toSet()
        if (unique.size != ids.size) throw BackupCorruptedException()
        return unique
    }

    private fun uniqueStrings(values: List<String>): Set<String> {
        val unique = values.toSet()
        if (unique.size != values.size) throw BackupCorruptedException()
        return unique
    }

    private fun requireKeywords(ids: List<Long>, known: Set<Long>) {
        if (ids.any { it !in known }) throw BackupCorruptedException()
    }

    private fun parseAssignmentSource(raw: String) {
        val ok = CategoryAssignmentSource.entries.any { it.name.equals(raw, ignoreCase = true) }
        if (!ok) throw BackupCorruptedException()
    }
}
