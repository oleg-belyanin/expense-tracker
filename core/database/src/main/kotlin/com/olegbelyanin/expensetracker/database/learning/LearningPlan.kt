package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource

internal data class LearningFingerprint(val normalizedName: String, val categoryId: Long, val locationId: Long?)

internal data class LearningPlan(
    val writeLearning: Boolean,
    val bumpLocationId: Long?,
    val unbumpLocationId: Long?,
    val feedbackType: String,
    val writeExactRule: Boolean,
    val transitionFromCategoryId: Long?,
)

internal object LearningPlanner {
    const val SOURCE_USER = "user"
    const val AUTO_ACCEPTED = "auto_accepted"
    const val EXPLICIT = "explicit"
    const val CORRECTION = "correction"

    fun plan(
        hadExpense: Boolean,
        previous: LearningFingerprint?,
        next: LearningFingerprint,
        source: CategoryAssignmentSource,
        interactive: Boolean,
    ): LearningPlan {
        val locationChanged = previous?.locationId != next.locationId
        val bumpLocationId = when {
            next.locationId == null -> null
            !hadExpense -> next.locationId
            locationChanged -> next.locationId
            else -> null
        }
        val unbumpLocationId = when {
            hadExpense && locationChanged -> previous?.locationId
            else -> null
        }
        val sameFingerprint = previous == next
        if (!interactive) {
            return LearningPlan(
                writeLearning = false,
                bumpLocationId = bumpLocationId,
                unbumpLocationId = unbumpLocationId,
                feedbackType = AUTO_ACCEPTED,
                writeExactRule = false,
                transitionFromCategoryId = null,
            )
        }
        if (hadExpense && sameFingerprint) {
            return LearningPlan(
                writeLearning = false,
                bumpLocationId = null,
                unbumpLocationId = null,
                feedbackType = AUTO_ACCEPTED,
                writeExactRule = source == CategoryAssignmentSource.EXPLICIT,
                transitionFromCategoryId = null,
            )
        }
        val previousCategoryId = previous?.categoryId
        val categoryChanged = previousCategoryId != null && previousCategoryId != next.categoryId
        val feedbackType = when {
            categoryChanged -> CORRECTION
            source == CategoryAssignmentSource.EXPLICIT -> EXPLICIT
            else -> AUTO_ACCEPTED
        }
        return LearningPlan(
            writeLearning = true,
            bumpLocationId = bumpLocationId,
            unbumpLocationId = unbumpLocationId,
            feedbackType = feedbackType,
            writeExactRule = feedbackType == EXPLICIT || feedbackType == CORRECTION,
            transitionFromCategoryId = if (categoryChanged) previousCategoryId else null,
        )
    }
}
