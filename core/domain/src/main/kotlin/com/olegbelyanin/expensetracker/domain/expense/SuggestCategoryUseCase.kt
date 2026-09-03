package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.model.CategorizationCandidate
import com.olegbelyanin.expensetracker.model.CategorizationResult
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource

/**
 * Заглушка до этапа B3: без CategorizationEngine всегда отдаёт fallback «Прочее».
 * Контракт уже совпадает с тем, что будет читать форма расхода.
 */
class SuggestCategoryUseCase(private val categories: CategoryRepository) {
    suspend operator fun invoke(name: String, locationName: String?): CategorizationResult {
        val fallback = categories.requireFallback()
        return CategorizationResult(
            selectedCategoryId = fallback.id,
            orderedCandidates = listOf(CategorizationCandidate(fallback.id, 0.0)),
            source = CategoryAssignmentSource.FALLBACK,
            confidence = 0.0,
            matchedFeatures = emptyList(),
            usedFallback = true,
        )
    }
}
