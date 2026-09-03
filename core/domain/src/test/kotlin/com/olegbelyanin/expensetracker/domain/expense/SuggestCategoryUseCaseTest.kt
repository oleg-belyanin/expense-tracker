package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.categorization.CategorizationCatalog
import com.olegbelyanin.expensetracker.categorization.CategorizationLookup
import com.olegbelyanin.expensetracker.categorization.CategorizationQuery
import com.olegbelyanin.expensetracker.categorization.CategorizationSnapshot
import com.olegbelyanin.expensetracker.categorization.CategoryVector
import com.olegbelyanin.expensetracker.categorization.ExactMatch
import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.KeywordKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestCategoryUseCaseTest {
    @Test
    fun usesEngineSequenceFromCatalogSnapshot() = runTest {
        val latte = KeywordFeature("латт", KeywordKind.WORD)
        val catalog = CategorizationCatalog { _, _ ->
            CategorizationLookup(
                query = CategorizationQuery("латт", listOf(latte)),
                snapshot = CategorizationSnapshot(
                    fallbackCategoryId = 10,
                    activeCategoryIds = setOf(2, 10),
                    seedExact = ExactMatch(2, CategoryVector.SOURCE_SEED),
                    featureVectors = mapOf(latte to CategoryVector(mapOf(2L to 0.9, 10L to 0.1))),
                ),
            )
        }
        val result = SuggestCategoryUseCase(catalog)("Латте", "Шоколадница")
        assertEquals(2, result.selectedCategoryId)
        assertEquals(CategoryAssignmentSource.PROBABILISTIC, result.source)
    }
}
