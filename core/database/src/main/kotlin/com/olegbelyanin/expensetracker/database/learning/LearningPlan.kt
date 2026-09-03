package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource

internal data class LearningFingerprint(val normalizedName: String, val categoryId: Long, val locationId: Long?)

internal data class LearningPlan(
    /** `learning_example`, `name_category_context` и пользовательские агрегаты. */
    val writeLearning: Boolean,
    val bumpLocationId: Long?,
    val unbumpLocationId: Long?,
    val feedbackType: String,
    val writeExactRule: Boolean,
    val transitionFromCategoryId: Long?,
)

internal object LearningPlanner {
    const val SOURCE_USER = "user"
    const val SOURCE_SEED = "seed"
    const val AUTO_ACCEPTED = "auto_accepted"
    const val EXPLICIT = "explicit"
    const val CORRECTION = "correction"

    fun plan(
        hadExpense: Boolean,
        previous: LearningFingerprint?,
        next: LearningFingerprint,
        source: CategoryAssignmentSource,
        /** Импорт и фоновый пересчёт: категория пишется, обучение — нет (§11.1 AD-CAT-001). */
        interactive: Boolean,
        proposedCategoryId: Long? = null,
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
        val savedCategoryChanged = previousCategoryId != null && previousCategoryId != next.categoryId
        val proposedCorrection = !hadExpense && proposedCategoryId != null && proposedCategoryId != next.categoryId
        val feedbackType = when {
            savedCategoryChanged || proposedCorrection -> CORRECTION
            source == CategoryAssignmentSource.EXPLICIT -> EXPLICIT
            else -> AUTO_ACCEPTED
        }
        return LearningPlan(
            writeLearning = true,
            bumpLocationId = bumpLocationId,
            unbumpLocationId = unbumpLocationId,
            feedbackType = feedbackType,
            writeExactRule = writesExactRule(feedbackType),
            transitionFromCategoryId = when {
                savedCategoryChanged -> previousCategoryId
                proposedCorrection -> proposedCategoryId
                else -> null
            },
        )
    }

    fun writesExactRule(feedbackType: String): Boolean = feedbackType == EXPLICIT || feedbackType == CORRECTION

    fun shouldWriteNameContext(normalizedName: String): Boolean = normalizedName.isNotEmpty()

    fun exactRuleSource(feedbackType: String): String = if (feedbackType == CORRECTION) CORRECTION else EXPLICIT

    fun shouldUpsertExactRule(
        normalizedName: String,
        existingCategoryId: Long?,
        existingSource: String?,
        categoryId: Long,
        ruleSource: String,
    ): Boolean {
        if (normalizedName.isEmpty()) return false
        if (existingCategoryId == null) return true
        if (existingCategoryId != categoryId) return true
        return existingSource != ruleSource
    }
}
