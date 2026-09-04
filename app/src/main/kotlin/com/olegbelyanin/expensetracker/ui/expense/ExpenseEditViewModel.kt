package com.olegbelyanin.expensetracker.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.LocationRepository
import com.olegbelyanin.expensetracker.domain.category.CategoryNameError
import com.olegbelyanin.expensetracker.domain.category.CreateCategoryResult
import com.olegbelyanin.expensetracker.domain.category.CreateCategoryUseCase
import com.olegbelyanin.expensetracker.domain.expense.AmountFieldError
import com.olegbelyanin.expensetracker.domain.expense.CategoryAssignment
import com.olegbelyanin.expensetracker.domain.expense.DeleteExpenseUseCase
import com.olegbelyanin.expensetracker.domain.expense.ExpenseInputValidator
import com.olegbelyanin.expensetracker.domain.expense.NameFieldError
import com.olegbelyanin.expensetracker.domain.expense.SaveExpenseCommand
import com.olegbelyanin.expensetracker.domain.expense.SaveExpenseResult
import com.olegbelyanin.expensetracker.domain.expense.SaveExpenseUseCase
import com.olegbelyanin.expensetracker.domain.expense.SuggestCategoryUseCase
import com.olegbelyanin.expensetracker.domain.expense.SuggestExpenseNamesUseCase
import com.olegbelyanin.expensetracker.domain.expense.SuggestLocationsUseCase
import com.olegbelyanin.expensetracker.model.CategorizationResult
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.ExpenseNameSuggestion
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.ui.components.KeypadKey
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

enum class ExpenseEditSheet {
    None,
    Amount,
    Date,
    Category,
    CreateCategory,
    DeleteConfirm,
}

enum class ExpenseEditNotice {
    SaveFailed,
    DeleteFailed,
}

data class ExpenseEditUiState(
    val isEdit: Boolean,
    val isReady: Boolean = false,
    val missing: Boolean = false,
    val amountInput: String = "",
    val spentAt: LocalDate,
    val name: String = "",
    val locationName: String = "",
    val comment: String = "",
    val category: Category? = null,
    val categories: List<Category> = emptyList(),
    val suggestion: CategorizationResult? = null,
    val categoryLocked: Boolean = false,
    val originalSource: CategoryAssignmentSource? = null,
    val locationSuggestions: List<Location> = emptyList(),
    val locationFocused: Boolean = false,
    val nameSuggestions: List<ExpenseNameSuggestion> = emptyList(),
    val nameFocused: Boolean = false,
    val sheet: ExpenseEditSheet = ExpenseEditSheet.None,
    val categoryQuery: String = "",
    val createCategoryName: String = "",
    val createCategoryError: CategoryNameError? = null,
    val amountTouched: Boolean = false,
    val nameTouched: Boolean = false,
    val attemptedSave: Boolean = false,
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val creatingCategory: Boolean = false,
    val notice: ExpenseEditNotice? = null,
)

class ExpenseEditViewModel(
    private val expenseId: String?,
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val locations: LocationRepository,
    private val saveExpense: suspend (SaveExpenseCommand) -> SaveExpenseResult,
    private val deleteExpense: suspend (String) -> Unit,
    private val suggestLocations: suspend (String) -> List<Location>,
    private val suggestNames: suspend (String) -> List<ExpenseNameSuggestion>,
    private val suggestCategory: suspend (String, String?) -> CategorizationResult,
    private val createCategory: suspend (String) -> CreateCategoryResult,
    private val validator: ExpenseInputValidator,
    private val clock: Clock,
    private val zoneId: ZoneId,
) : ViewModel() {
    private val today: LocalDate = LocalDate.now(clock.withZone(zoneId))
    private val _state = MutableStateFlow(
        ExpenseEditUiState(isEdit = expenseId != null, spentAt = today),
    )
    val state: StateFlow<ExpenseEditUiState> = _state.asStateFlow()

    private var suggestJob: Job? = null
    private var locationJob: Job? = null
    private var nameJob: Job? = null
    private var noticeJob: Job? = null

    init {
        viewModelScope.launch {
            categories.observeActiveCategories().collect { rows ->
                _state.update { it.copy(categories = rows) }
            }
        }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (expenseId == null) {
            applySuggestion(suggest(name = "", locationName = null), replaceCategory = true)
            _state.update { it.copy(isReady = true) }
            return
        }
        val expense = expenses.get(expenseId)
        if (expense == null) {
            _state.update { it.copy(isReady = true, missing = true) }
            return
        }
        val category = categories.findById(expense.categoryId)
        val locationName = expense.locationId?.let { locations.findById(it)?.name }.orEmpty()
        _state.update {
            it.copy(
                isReady = true,
                amountInput = ExpenseFormat.moneyInput(expense.amount.minor),
                spentAt = expense.spentAt.atZone(zoneId).toLocalDate(),
                name = expense.name,
                locationName = locationName,
                comment = expense.comment.orEmpty(),
                category = category,
                categoryLocked = expense.categoryAssignmentSource == CategoryAssignmentSource.EXPLICIT,
                originalSource = expense.categoryAssignmentSource,
            )
        }
        applySuggestion(suggest(expense.name, locationName), replaceCategory = false)
    }

    fun onAmountKey(key: KeypadKey) {
        _state.update {
            it.copy(
                amountInput = AmountKeypadInput.apply(it.amountInput, key),
                amountTouched = true,
            )
        }
    }

    fun onOpenSheet(sheet: ExpenseEditSheet) {
        _state.update {
            val createName = if (sheet == ExpenseEditSheet.CreateCategory) {
                it.categoryQuery.trim().ifEmpty { it.createCategoryName }
            } else {
                it.createCategoryName
            }
            it.copy(sheet = sheet, createCategoryName = createName, createCategoryError = null)
        }
        if (sheet == ExpenseEditSheet.Category) {
            refreshSuggestion(replaceCategory = false)
        }
    }

    fun onDismissSheet() {
        _state.update { it.copy(sheet = ExpenseEditSheet.None) }
    }

    fun onDateSelected(date: LocalDate) {
        if (date.isAfter(today)) return
        _state.update { it.copy(spentAt = date, sheet = ExpenseEditSheet.None) }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, nameTouched = true, nameFocused = true) }
        scheduleNameAndCategory()
    }

    fun onNameFocus(focused: Boolean) {
        _state.update { it.copy(nameFocused = focused) }
        if (focused) {
            scheduleNames(_state.value.name)
        }
    }

    fun onNameSuggestion(suggestion: ExpenseNameSuggestion) {
        _state.update {
            it.copy(
                name = suggestion.name,
                nameSuggestions = emptyList(),
                nameFocused = false,
                nameTouched = true,
            )
        }
        scheduleSuggest()
    }

    fun onCommentChange(value: String) {
        _state.update { it.copy(comment = value) }
    }

    fun onLocationChange(value: String) {
        _state.update { it.copy(locationName = value, locationFocused = true) }
        scheduleLocations(value)
        scheduleSuggest()
    }

    fun onLocationFocus(focused: Boolean) {
        _state.update { it.copy(locationFocused = focused) }
        if (focused) {
            scheduleLocations(_state.value.locationName)
        }
    }

    fun onLocationSuggestion(location: Location) {
        _state.update {
            it.copy(
                locationName = location.name,
                locationSuggestions = emptyList(),
                locationFocused = false,
            )
        }
        scheduleSuggest()
    }

    fun onCategoryQueryChange(value: String) {
        _state.update { it.copy(categoryQuery = value) }
    }

    fun onSelectCategory(category: Category) {
        _state.update {
            it.copy(
                category = category,
                categoryLocked = true,
                sheet = ExpenseEditSheet.None,
                categoryQuery = "",
            )
        }
    }

    fun onCreateCategoryNameChange(value: String) {
        _state.update { it.copy(createCategoryName = value, createCategoryError = null) }
    }

    fun onCreateCategory() {
        val name = _state.value.createCategoryName
        _state.update { it.copy(creatingCategory = true, createCategoryError = null) }
        viewModelScope.launch {
            try {
                when (val result = createCategory(name)) {
                    is CreateCategoryResult.Success -> {
                        _state.update { it.copy(creatingCategory = false) }
                        onSelectCategory(result.category)
                    }

                    is CreateCategoryResult.Invalid ->
                        _state.update {
                            it.copy(creatingCategory = false, createCategoryError = result.error)
                        }
                }
            } catch (error: CancellationException) {
                _state.update { it.copy(creatingCategory = false) }
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(creatingCategory = false) }
                showNotice(ExpenseEditNotice.SaveFailed)
            }
        }
    }

    fun onSave(onSaved: (String) -> Unit) {
        val current = _state.value
        val category = current.category ?: return
        _state.update { it.copy(attemptedSave = true, saving = true, notice = null) }
        viewModelScope.launch {
            try {
                val source = CategoryAssignment.sourceForSave(
                    userPicked = current.categoryLocked,
                    suggestionSource = current.suggestion?.source,
                    originalSource = current.originalSource,
                )
                val result = saveExpense(
                    SaveExpenseCommand(
                        id = expenseId,
                        amountInput = current.amountInput,
                        spentAt = current.spentAt,
                        name = current.name,
                        categoryId = category.id,
                        locationName = current.locationName.ifBlank { null },
                        comment = current.comment,
                        categoryAssignmentSource = source,
                        proposedCategoryId = current.suggestion?.selectedCategoryId,
                    ),
                )
                when (result) {
                    is SaveExpenseResult.Success -> onSaved(result.expense.id)
                    is SaveExpenseResult.Invalid -> _state.update { it.copy(saving = false) }
                }
            } catch (error: CancellationException) {
                _state.update { it.copy(saving = false) }
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(saving = false) }
                showNotice(ExpenseEditNotice.SaveFailed)
            }
        }
    }

    fun onConfirmDelete(onDeleted: () -> Unit) {
        val id = expenseId ?: return
        _state.update { it.copy(deleting = true, notice = null) }
        viewModelScope.launch {
            try {
                deleteExpense(id)
                onDeleted()
            } catch (error: CancellationException) {
                _state.update { it.copy(deleting = false) }
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(deleting = false) }
                showNotice(ExpenseEditNotice.DeleteFailed)
            }
        }
    }

    fun onDismissNotice() {
        noticeJob?.cancel()
        _state.update { it.copy(notice = null) }
    }

    fun amountError(): AmountFieldError? {
        val current = _state.value
        if (!current.amountTouched && !current.attemptedSave) return null
        return validator.validate(command(current), today).amount
    }

    fun nameError(): NameFieldError? {
        val current = _state.value
        if (!current.nameTouched && !current.attemptedSave) return null
        return validator.validate(command(current), today).name
    }

    fun canSave(): Boolean {
        val current = _state.value
        if (current.category == null || current.saving) return false
        return !validator.validate(command(current), today).hasErrors
    }

    private fun command(state: ExpenseEditUiState) = SaveExpenseCommand(
        id = expenseId,
        amountInput = state.amountInput,
        spentAt = state.spentAt,
        name = state.name,
        categoryId = state.category?.id ?: -1,
        locationName = state.locationName,
        comment = state.comment,
        categoryAssignmentSource = CategoryAssignmentSource.FALLBACK,
    )

    private fun scheduleSuggest() {
        suggestJob?.cancel()
        suggestJob =
            viewModelScope.launch {
                delay(150)
                applySuggestion(suggestCurrent(), replaceCategory = true)
            }
    }

    private fun scheduleNameAndCategory() {
        nameJob?.cancel()
        suggestJob?.cancel()
        suggestJob =
            viewModelScope.launch {
                delay(150)
                val typed = _state.value.name
                val names = visibleNameSuggestions(
                    typed,
                    suggestNames(typed).take(SuggestExpenseNamesUseCase.limitFor(typed)),
                )
                _state.update { it.copy(nameSuggestions = names) }
                val lookup = uniqueCompletionName(typed, names) ?: typed
                applySuggestion(suggest(lookup, _state.value.locationName), replaceCategory = true)
            }
    }

    private fun uniqueCompletionName(typed: String, names: List<ExpenseNameSuggestion>): String? {
        if (typed.trim().length < 2) return null
        return names.distinctBy { it.normalizedName }.singleOrNull()?.name
    }

    private fun refreshSuggestion(replaceCategory: Boolean) {
        suggestJob?.cancel()
        suggestJob =
            viewModelScope.launch {
                applySuggestion(suggestCurrent(), replaceCategory = replaceCategory)
            }
    }

    private fun scheduleLocations(query: String) {
        locationJob?.cancel()
        locationJob =
            viewModelScope.launch {
                delay(150)
                val suggestions = suggestLocations(query).take(SuggestLocationsUseCase.limitFor(query))
                _state.update { it.copy(locationSuggestions = suggestions) }
            }
    }

    private fun scheduleNames(query: String) {
        nameJob?.cancel()
        nameJob =
            viewModelScope.launch {
                delay(150)
                val suggestions = visibleNameSuggestions(
                    query,
                    suggestNames(query).take(SuggestExpenseNamesUseCase.limitFor(query)),
                )
                _state.update { it.copy(nameSuggestions = suggestions) }
            }
    }

    private fun visibleNameSuggestions(
        query: String,
        suggestions: List<ExpenseNameSuggestion>,
    ): List<ExpenseNameSuggestion> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return suggestions
        if (suggestions.any { it.name.equals(trimmed, ignoreCase = true) }) return emptyList()
        return suggestions
    }

    private suspend fun suggestCurrent(): CategorizationResult {
        val current = _state.value
        return suggest(current.name, current.locationName)
    }

    private suspend fun suggest(name: String, locationName: String?): CategorizationResult =
        suggestCategory(name, locationName?.ifBlank { null })

    private fun showNotice(notice: ExpenseEditNotice) {
        _state.update { it.copy(notice = notice) }
        noticeJob?.cancel()
        noticeJob =
            viewModelScope.launch {
                delay(TOAST_MS)
                if (_state.value.notice == notice) {
                    _state.update { it.copy(notice = null) }
                }
            }
    }

    private suspend fun applySuggestion(result: CategorizationResult, replaceCategory: Boolean) {
        val suggested = categories.findById(result.selectedCategoryId)
        _state.update { current ->
            current.copy(
                suggestion = result,
                category =
                if (CategorySuggestionUi.replaceAutofill(current.categoryLocked, replaceCategory)) {
                    suggested ?: current.category
                } else {
                    current.category
                },
            )
        }
    }

    companion object {
        private const val TOAST_MS = 4_000L

        fun factory(
            expenseId: String?,
            expenses: ExpenseRepository,
            categories: CategoryRepository,
            locations: LocationRepository,
            saveExpense: SaveExpenseUseCase,
            deleteExpense: DeleteExpenseUseCase,
            suggestLocations: SuggestLocationsUseCase,
            suggestNames: SuggestExpenseNamesUseCase,
            suggestCategory: SuggestCategoryUseCase,
            createCategory: CreateCategoryUseCase,
            clock: Clock,
            zoneId: ZoneId,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ExpenseEditViewModel(
                    expenseId = expenseId,
                    expenses = expenses,
                    categories = categories,
                    locations = locations,
                    saveExpense = saveExpense::invoke,
                    deleteExpense = deleteExpense::invoke,
                    suggestLocations = { query ->
                        suggestLocations(query, SuggestLocationsUseCase.limitFor(query))
                    },
                    suggestNames = { query ->
                        suggestNames(query, SuggestExpenseNamesUseCase.limitFor(query))
                    },
                    suggestCategory = { name, locationName -> suggestCategory(name, locationName) },
                    createCategory = { name -> createCategory(name) },
                    validator = ExpenseInputValidator(),
                    clock = clock,
                    zoneId = zoneId,
                ) as T
            }
        }
    }
}
