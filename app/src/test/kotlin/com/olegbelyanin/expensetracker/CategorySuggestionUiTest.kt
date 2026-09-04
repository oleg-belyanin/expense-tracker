package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.model.CategorizationCandidate
import com.olegbelyanin.expensetracker.model.CategorizationResult
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.KeywordKind
import com.olegbelyanin.expensetracker.model.MatchedFeature
import com.olegbelyanin.expensetracker.ui.expense.CategorySourceCaption
import com.olegbelyanin.expensetracker.ui.expense.CategorySuggestionUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySuggestionUiTest {
    @Test
    fun userPickIsManualEvenIfEngineSuggested() {
        val caption = CategorySuggestionUi.caption(
            userPicked = true,
            result = result(CategoryAssignmentSource.PROBABILISTIC, 0.87),
        )
        assertEquals(CategorySourceCaption.Manual, caption)
    }

    @Test
    fun nameFeaturesUseDictionaryConfidence() {
        val caption = CategorySuggestionUi.caption(
            userPicked = false,
            result = result(
                CategoryAssignmentSource.PROBABILISTIC,
                0.87,
                MatchedFeature("капучино", KeywordKind.WORD, MatchedFeature.SOURCE_NAME),
            ),
        )
        assertEquals(CategorySourceCaption.Dictionary(0.87), caption)
    }

    @Test
    fun placeOnlyUsesPlaceCaption() {
        val caption = CategorySuggestionUi.caption(
            userPicked = false,
            result = result(
                CategoryAssignmentSource.PROBABILISTIC,
                0.64,
                MatchedFeature("кофейн", KeywordKind.PHRASE, MatchedFeature.SOURCE_LOCATION),
            ),
        )
        assertEquals(CategorySourceCaption.Place(0.64), caption)
    }

    @Test
    fun exactRuleAndFallbackHaveFixedCaptions() {
        assertEquals(
            CategorySourceCaption.UserRule,
            CategorySuggestionUi.caption(false, result(CategoryAssignmentSource.EXACT_USER, 1.0)),
        )
        assertEquals(
            CategorySourceCaption.Fallback,
            CategorySuggestionUi.caption(false, result(CategoryAssignmentSource.FALLBACK, 0.0, usedFallback = true)),
        )
    }

    @Test
    fun savedExpenseKeepsOriginalSourceWithoutRecalculation() {
        assertEquals(
            CategorySourceCaption.UserRule,
            CategorySuggestionUi.caption(false, null, CategoryAssignmentSource.EXACT_USER),
        )
        assertEquals(
            CategorySourceCaption.Dictionary(null),
            CategorySuggestionUi.caption(false, null, CategoryAssignmentSource.PROBABILISTIC),
        )
    }

    @Test
    fun autofillDoesNotReplaceLockedOrEditLoad() {
        assertTrue(CategorySuggestionUi.replaceAutofill(locked = false, replaceCategory = true))
        assertTrue(!CategorySuggestionUi.replaceAutofill(locked = true, replaceCategory = true))
        assertTrue(!CategorySuggestionUi.replaceAutofill(locked = false, replaceCategory = false))
    }

    @Test
    fun rankedCandidatesTakeTopThreeWithScore() {
        val ranked = CategorySuggestionUi.rankedCandidates(
            result(
                CategoryAssignmentSource.PROBABILISTIC,
                0.87,
                candidates = listOf(
                    CategorizationCandidate(2, 0.87),
                    CategorizationCandidate(1, 0.09),
                    CategorizationCandidate(5, 0.02),
                    CategorizationCandidate(4, 0.01),
                    CategorizationCandidate(10, 0.0),
                ),
            ),
        )
        assertEquals(listOf(2L, 1L, 5L), ranked.map { it.categoryId })
        assertTrue(
            CategorySuggestionUi.rankedCandidates(
                result(CategoryAssignmentSource.FALLBACK, 0.0, usedFallback = true),
            ).isEmpty(),
        )
        assertEquals(
            listOf(4L, 8L, 10L),
            CategorySuggestionUi.rankedCandidates(
                result(
                    CategoryAssignmentSource.FALLBACK,
                    0.0,
                    usedFallback = true,
                    candidates = listOf(
                        CategorizationCandidate(4, 0.45),
                        CategorizationCandidate(8, 0.45),
                        CategorizationCandidate(10, 0.10),
                    ),
                ),
            ).map { it.categoryId },
        )
    }
}

private fun result(
    source: CategoryAssignmentSource,
    confidence: Double,
    vararg features: MatchedFeature,
    usedFallback: Boolean = false,
    candidates: List<CategorizationCandidate> = emptyList(),
) = CategorizationResult(
    selectedCategoryId = 2,
    orderedCandidates = candidates,
    source = source,
    confidence = confidence,
    matchedFeatures = features.toList(),
    usedFallback = usedFallback,
)
