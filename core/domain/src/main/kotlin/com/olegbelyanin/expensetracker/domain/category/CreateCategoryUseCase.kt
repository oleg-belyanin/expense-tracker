package com.olegbelyanin.expensetracker.domain.category

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.CategoryIcons
import com.olegbelyanin.expensetracker.model.CategoryPalette

enum class CategoryNameError {
    EMPTY,
    DUPLICATE,
}

sealed interface CreateCategoryResult {
    data class Success(val category: Category) : CreateCategoryResult

    data class Invalid(val error: CategoryNameError) : CreateCategoryResult
}

class CreateCategoryUseCase(
    private val categories: CategoryRepository,
    private val suggestIcon: (String) -> String = { CategoryIcons.LETTER },
) {
    suspend operator fun invoke(rawName: String, color: String? = null, icon: String? = null): CreateCategoryResult {
        val name = rawName.trim()
        if (name.none { it.isLetterOrDigit() }) {
            return CreateCategoryResult.Invalid(CategoryNameError.EMPTY)
        }
        return try {
            CreateCategoryResult.Success(
                categories.createUserCategory(
                    name = name,
                    color = resolveColor(color),
                    icon = resolveIcon(name, icon),
                ),
            )
        } catch (_: DuplicateCategoryNameException) {
            CreateCategoryResult.Invalid(CategoryNameError.DUPLICATE)
        } catch (_: EmptyCategoryNameException) {
            CreateCategoryResult.Invalid(CategoryNameError.EMPTY)
        }
    }

    private fun resolveColor(color: String?): String {
        val value = color?.trim().orEmpty()
        return if (HEX.matches(value)) value else CategoryPalette.DEFAULT
    }

    private fun resolveIcon(name: String, icon: String?): String {
        val raw = icon?.trim().orEmpty()
        return if (raw.isEmpty()) suggestIcon(name) else CategoryIcons.canonicalize(raw)
    }

    companion object {
        const val DEFAULT_COLOR = CategoryPalette.DEFAULT
        const val DEFAULT_ICON = CategoryIcons.LETTER
        private val HEX = Regex("^#[0-9A-Fa-f]{6}$")
    }
}
