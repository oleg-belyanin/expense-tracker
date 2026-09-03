package com.olegbelyanin.expensetracker.domain.category

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.model.Category

sealed interface ArchiveCategoryResult {
    data class Success(val category: Category) : ArchiveCategoryResult

    data class Rejected(val error: CategoryMutationError) : ArchiveCategoryResult
}

class ArchiveCategoryUseCase(private val categories: CategoryRepository) {
    suspend operator fun invoke(id: Long): ArchiveCategoryResult = try {
        ArchiveCategoryResult.Success(categories.archive(id))
    } catch (_: CategoryNotFoundException) {
        ArchiveCategoryResult.Rejected(CategoryMutationError.NOT_FOUND)
    } catch (_: FallbackCategoryProtectedException) {
        ArchiveCategoryResult.Rejected(CategoryMutationError.FALLBACK_PROTECTED)
    }
}

class RestoreCategoryUseCase(private val categories: CategoryRepository) {
    suspend operator fun invoke(id: Long): ArchiveCategoryResult = try {
        ArchiveCategoryResult.Success(categories.restore(id))
    } catch (_: CategoryNotFoundException) {
        ArchiveCategoryResult.Rejected(CategoryMutationError.NOT_FOUND)
    }
}
