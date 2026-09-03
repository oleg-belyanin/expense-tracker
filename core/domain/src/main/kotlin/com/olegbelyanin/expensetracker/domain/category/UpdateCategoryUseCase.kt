package com.olegbelyanin.expensetracker.domain.category

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.CategoryIcons

enum class CategoryMutationError {
    NOT_FOUND,
    BUILTIN,
    FALLBACK_PROTECTED,
}

sealed interface UpdateCategoryResult {
    data class Success(val category: Category) : UpdateCategoryResult

    data class InvalidName(val error: CategoryNameError) : UpdateCategoryResult

    data class Rejected(val error: CategoryMutationError) : UpdateCategoryResult
}

class UpdateCategoryUseCase(
    private val categories: CategoryRepository,
    private val suggestIcon: (String) -> String = { CategoryIcons.LETTER },
) {
    suspend operator fun invoke(
        id: Long,
        rawName: String,
        color: String? = null,
        icon: String? = null,
    ): UpdateCategoryResult {
        val current = categories.findById(id)
            ?: return UpdateCategoryResult.Rejected(CategoryMutationError.NOT_FOUND)
        if (current.isBuiltin) {
            return UpdateCategoryResult.Rejected(CategoryMutationError.BUILTIN)
        }
        val name = rawName.trim()
        if (name.none { it.isLetterOrDigit() }) {
            return UpdateCategoryResult.InvalidName(CategoryNameError.EMPTY)
        }
        val resolvedColor = color?.trim()?.takeIf { HEX.matches(it) } ?: current.color
        val resolvedIcon = icon?.trim()?.takeIf { it.isNotEmpty() }?.let(CategoryIcons::canonicalize)
            ?: current.icon.ifBlank { suggestIcon(name) }
        return try {
            UpdateCategoryResult.Success(
                categories.updateUserCategory(id, name, resolvedColor, resolvedIcon),
            )
        } catch (_: DuplicateCategoryNameException) {
            UpdateCategoryResult.InvalidName(CategoryNameError.DUPLICATE)
        } catch (_: EmptyCategoryNameException) {
            UpdateCategoryResult.InvalidName(CategoryNameError.EMPTY)
        } catch (_: CategoryNotFoundException) {
            UpdateCategoryResult.Rejected(CategoryMutationError.NOT_FOUND)
        } catch (_: BuiltinCategoryLockedException) {
            UpdateCategoryResult.Rejected(CategoryMutationError.BUILTIN)
        }
    }

    companion object {
        private val HEX = Regex("^#[0-9A-Fa-f]{6}$")
    }
}
