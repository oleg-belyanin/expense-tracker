package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource

/**
 * Источник, который уходит в persist. Exact rule пишется только для [EXPLICIT]
 * или исправления категории (§11.2 AD-CAT-001).
 */
object CategoryAssignment {
    fun sourceForSave(
        userPicked: Boolean,
        suggestionSource: CategoryAssignmentSource?,
        originalSource: CategoryAssignmentSource?,
    ): CategoryAssignmentSource = when {
        userPicked -> CategoryAssignmentSource.EXPLICIT
        suggestionSource != null -> suggestionSource
        originalSource != null -> originalSource
        else -> CategoryAssignmentSource.FALLBACK
    }
}
