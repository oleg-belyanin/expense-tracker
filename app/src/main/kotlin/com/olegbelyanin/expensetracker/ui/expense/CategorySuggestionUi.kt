package com.olegbelyanin.expensetracker.ui.expense

import com.olegbelyanin.expensetracker.model.CategorizationCandidate
import com.olegbelyanin.expensetracker.model.CategorizationResult
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.MatchedFeature

sealed interface CategorySourceCaption {
    data object Manual : CategorySourceCaption

    data object Fallback : CategorySourceCaption

    data object UserRule : CategorySourceCaption

    data class Dictionary(val confidence: Double?) : CategorySourceCaption

    data class Place(val confidence: Double) : CategorySourceCaption
}

object CategorySuggestionUi {
    const val RANKED_LIMIT = 3

    fun caption(
        userPicked: Boolean,
        result: CategorizationResult?,
        originalSource: CategoryAssignmentSource? = null,
    ): CategorySourceCaption {
        if (userPicked) return CategorySourceCaption.Manual
        if (result != null) return captionForResult(result)
        return when (originalSource) {
            CategoryAssignmentSource.EXACT_USER -> CategorySourceCaption.UserRule
            CategoryAssignmentSource.EXPLICIT -> CategorySourceCaption.Manual
            CategoryAssignmentSource.PROBABILISTIC -> CategorySourceCaption.Dictionary(null)
            CategoryAssignmentSource.FALLBACK, null -> CategorySourceCaption.Fallback
        }
    }

    fun rankedCandidates(result: CategorizationResult?, limit: Int = RANKED_LIMIT): List<CategorizationCandidate> {
        if (result == null) return emptyList()
        return result.orderedCandidates.filter { it.score > 0.0 }.take(limit)
    }

    fun replaceAutofill(locked: Boolean, replaceCategory: Boolean): Boolean = replaceCategory && !locked

    private fun captionForResult(result: CategorizationResult): CategorySourceCaption = when (result.source) {
        CategoryAssignmentSource.EXACT_USER -> CategorySourceCaption.UserRule

        CategoryAssignmentSource.EXPLICIT -> CategorySourceCaption.Manual

        CategoryAssignmentSource.FALLBACK -> CategorySourceCaption.Fallback

        CategoryAssignmentSource.PROBABILISTIC -> {
            val hasName = result.matchedFeatures.any { it.source == MatchedFeature.SOURCE_NAME }
            val hasPlace = result.matchedFeatures.any { it.source == MatchedFeature.SOURCE_LOCATION }
            if (hasPlace && !hasName) {
                CategorySourceCaption.Place(result.confidence)
            } else {
                CategorySourceCaption.Dictionary(result.confidence)
            }
        }
    }
}
