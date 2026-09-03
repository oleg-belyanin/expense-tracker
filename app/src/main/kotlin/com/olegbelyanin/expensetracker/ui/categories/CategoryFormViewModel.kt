package com.olegbelyanin.expensetracker.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.olegbelyanin.expensetracker.categorization.CategoryIconSuggester
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.category.CategoryNameError
import com.olegbelyanin.expensetracker.domain.category.CreateCategoryResult
import com.olegbelyanin.expensetracker.domain.category.CreateCategoryUseCase
import com.olegbelyanin.expensetracker.domain.category.UpdateCategoryResult
import com.olegbelyanin.expensetracker.domain.category.UpdateCategoryUseCase
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.CategoryIcons
import com.olegbelyanin.expensetracker.model.CategoryPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryFormUiState(
    val isEdit: Boolean,
    val isReady: Boolean = false,
    val missing: Boolean = false,
    val draft: CategoryFormDraft = CategoryFormDraft(),
    val categories: List<Category> = emptyList(),
    val suggestedIcon: String = CategoryIcons.LETTER,
    val saving: Boolean = false,
)

class CategoryFormViewModel(
    private val categoryId: Long?,
    private val categories: CategoryRepository,
    private val createCategory: CreateCategoryUseCase,
    private val updateCategory: UpdateCategoryUseCase,
    private val iconSuggester: CategoryIconSuggester,
    private val normalizer: TextNormalizer,
) : ViewModel() {
    private val _state = MutableStateFlow(CategoryFormUiState(isEdit = categoryId != null))
    val state: StateFlow<CategoryFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categories.observeAll().collect { rows ->
                _state.update { it.copy(categories = rows) }
            }
        }
        viewModelScope.launch { load() }
    }

    fun onNameChange(value: String) {
        val suggested = iconSuggester.suggest(value)
        _state.update {
            it.copy(
                draft = it.draft.withName(value, suggested),
                suggestedIcon = suggested,
            )
        }
    }

    fun onColor(value: String) {
        _state.update { it.copy(draft = it.draft.withColor(value)) }
    }

    fun onIcon(value: String) {
        _state.update { it.copy(draft = it.draft.withIcon(value)) }
    }

    fun nameError(): CategoryNameError? = _state.value.draft.nameError(isDuplicate(_state.value))

    fun canSave(): Boolean {
        val current = _state.value
        return !current.saving && current.draft.canSave(isDuplicate(current))
    }

    fun onSave(onSaved: () -> Unit) {
        val current = _state.value
        _state.update { it.copy(draft = it.draft.markAttempted(), saving = true) }
        if (!current.draft.canSave(isDuplicate(current))) {
            _state.update { it.copy(saving = false) }
            return
        }
        viewModelScope.launch {
            val draft = _state.value.draft
            val ok =
                if (categoryId == null) {
                    when (createCategory(draft.name, draft.color, draft.icon)) {
                        is CreateCategoryResult.Success -> true
                        is CreateCategoryResult.Invalid -> false
                    }
                } else {
                    when (updateCategory(categoryId, draft.name, draft.color, draft.icon)) {
                        is UpdateCategoryResult.Success -> true

                        is UpdateCategoryResult.InvalidName,
                        is UpdateCategoryResult.Rejected,
                        -> false
                    }
                }
            if (ok) {
                onSaved()
            } else {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private suspend fun load() {
        if (categoryId == null) {
            _state.update { it.copy(isReady = true) }
            return
        }
        val category = categories.findById(categoryId)
        if (category == null || category.isBuiltin) {
            _state.update { it.copy(isReady = true, missing = true) }
            return
        }
        val icon = CategoryIcons.canonicalize(category.icon)
        val color =
            if (category.color in CategoryPalette.swatches) {
                category.color
            } else {
                CategoryPalette.swatchForIcon(icon)
            }
        _state.update {
            it.copy(
                isReady = true,
                suggestedIcon = iconSuggester.suggest(category.name),
                draft =
                CategoryFormDraft(
                    name = category.name,
                    color = color,
                    icon = icon,
                    iconManual = true,
                    colorManual = true,
                ),
            )
        }
    }

    private fun isDuplicate(state: CategoryFormUiState): Boolean {
        val normalized = normalizer.analyze(state.draft.name).normalizedName
        if (normalized.isEmpty()) return false
        val clash = state.categories.firstOrNull { it.normalizedName == normalized && it.id != categoryId }
        return when {
            clash == null -> false
            categoryId == null -> clash.isActive
            else -> true
        }
    }

    companion object {
        fun factory(
            categoryId: Long?,
            categories: CategoryRepository,
            createCategory: CreateCategoryUseCase,
            updateCategory: UpdateCategoryUseCase,
            iconSuggester: CategoryIconSuggester,
            normalizer: TextNormalizer,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CategoryFormViewModel(
                    categoryId = categoryId,
                    categories = categories,
                    createCategory = createCategory,
                    updateCategory = updateCategory,
                    iconSuggester = iconSuggester,
                    normalizer = normalizer,
                ) as T
            }
        }
    }
}
