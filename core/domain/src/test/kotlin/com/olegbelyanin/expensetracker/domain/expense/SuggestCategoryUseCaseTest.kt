package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.categorization.CategorizationCatalog
import com.olegbelyanin.expensetracker.categorization.CategorizationLookup
import com.olegbelyanin.expensetracker.categorization.CategorizationQuery
import com.olegbelyanin.expensetracker.categorization.CategorizationSnapshot
import com.olegbelyanin.expensetracker.categorization.CategoryVector
import com.olegbelyanin.expensetracker.categorization.ExactMatch
import com.olegbelyanin.expensetracker.categorization.KeywordFeature
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
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

    @Test
    fun createdStomatologyCategoryAliasesPurchase() = runTest {
        val stomatologyId = 8L
        val healthId = 4L
        val otherId = 10L
        val normalizer = TextNormalizer()
        val categoryNormalized = normalizer.analyze("Стоматология").normalizedName
        val feature = KeywordFeature("стоматолог", KeywordKind.WORD)
        val catalog = CategorizationCatalog { name, _ ->
            val analysis = normalizer.analyze(name)
            CategorizationLookup(
                query = CategorizationQuery(analysis.normalizedName, analysis.features),
                snapshot = CategorizationSnapshot(
                    fallbackCategoryId = otherId,
                    activeCategoryIds = setOf(healthId, stomatologyId, otherId),
                    seedExact = ExactMatch(healthId, CategoryVector.SOURCE_SEED),
                    categoryAliasId = stomatologyId.takeIf { analysis.normalizedName == categoryNormalized },
                    featureVectors = mapOf(
                        feature to CategoryVector(
                            mapOf(healthId to 0.90, stomatologyId to 0.05, otherId to 0.05),
                        ),
                    ),
                ),
            )
        }
        val result = SuggestCategoryUseCase(catalog)("стоматология", null)
        assertEquals(categoryNormalized, normalizer.analyze("стоматология").normalizedName)
        assertEquals(stomatologyId, result.selectedCategoryId)
        assertEquals(CategoryAssignmentSource.PROBABILISTIC, result.source)
    }
}
