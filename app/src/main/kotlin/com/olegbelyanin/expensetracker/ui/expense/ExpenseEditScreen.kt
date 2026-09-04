package com.olegbelyanin.expensetracker.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.domain.category.CategoryNameError
import com.olegbelyanin.expensetracker.domain.expense.AmountFieldError
import com.olegbelyanin.expensetracker.domain.expense.NameFieldError
import com.olegbelyanin.expensetracker.model.CategorizationResult
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.ui.components.BottomSheetSize
import com.olegbelyanin.expensetracker.ui.components.ButtonTone
import com.olegbelyanin.expensetracker.ui.components.CategorySelector
import com.olegbelyanin.expensetracker.ui.components.CategorySelectorState
import com.olegbelyanin.expensetracker.ui.components.DestructiveDialog
import com.olegbelyanin.expensetracker.ui.components.ExpenseBottomSheet
import com.olegbelyanin.expensetracker.ui.components.ExpenseDatePicker
import com.olegbelyanin.expensetracker.ui.components.ExpenseToast
import com.olegbelyanin.expensetracker.ui.components.FormField
import com.olegbelyanin.expensetracker.ui.components.FormFieldKind
import com.olegbelyanin.expensetracker.ui.components.Keypad
import com.olegbelyanin.expensetracker.ui.components.PlaceAutocomplete
import com.olegbelyanin.expensetracker.ui.components.PlaceSuggestion
import com.olegbelyanin.expensetracker.ui.components.PrimaryButton
import com.olegbelyanin.expensetracker.ui.components.ScreenStatePanel
import com.olegbelyanin.expensetracker.ui.components.SearchField
import com.olegbelyanin.expensetracker.ui.components.StatePanelType
import com.olegbelyanin.expensetracker.ui.components.TextAction
import com.olegbelyanin.expensetracker.ui.components.ToastTone
import com.olegbelyanin.expensetracker.ui.components.toVisual
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ExpenseEditScreen(
    viewModel: ExpenseEditViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.missing) {
        if (state.missing) onBack()
    }
    if (!state.isReady || state.missing) {
        ScreenStatePanel(
            type = StatePanelType.Loading,
            title = stringResource(R.string.form_loading_title),
            description = stringResource(R.string.form_loading_description),
            modifier = modifier,
        )
        return
    }
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val category = state.category
    val visual = category?.toVisual(colors)
    val amountError = viewModel.amountError()
    val nameError = viewModel.nameError()
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = spacing.md, vertical = spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                    if (state.isEdit) {
                        stringResource(R.string.expense_edit_title)
                    } else {
                        stringResource(R.string.expense_new_title)
                    },
                    style = typography.headlineScreen,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextAction(label = stringResource(R.string.cancel), onClick = onBack)
            }
            Text(
                text =
                if (state.isEdit) {
                    stringResource(R.string.expense_edit_subtitle)
                } else {
                    stringResource(R.string.expense_new_subtitle)
                },
                style = typography.bodySecondary,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.xxs, bottom = spacing.xs),
            )
            Column(
                modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                FormField(
                    label = stringResource(R.string.field_amount),
                    value = amountFieldValue(state.amountInput),
                    onValueChange = {},
                    kind = FormFieldKind.Amount,
                    error = amountError?.toMessage(),
                    onClick = { viewModel.onOpenSheet(ExpenseEditSheet.Amount) },
                )
                FormField(
                    label = stringResource(R.string.field_date),
                    value = ExpenseFormat.formDate(state.spentAt, today),
                    onValueChange = {},
                    kind = FormFieldKind.Date,
                    onClick = { viewModel.onOpenSheet(ExpenseEditSheet.Date) },
                )
                PlaceAutocomplete(
                    query = state.name,
                    onQueryChange = viewModel::onNameChange,
                    label = stringResource(R.string.field_name),
                    error = nameError?.toMessage(),
                    suggestions =
                    if (state.nameFocused) {
                        state.nameSuggestions.map { suggestion ->
                            PlaceSuggestion(
                                name = suggestion.name,
                                detail = ExpenseFormat.nameDetail(suggestion, today, zoneId),
                            )
                        }
                    } else {
                        emptyList()
                    },
                    onSuggestionClick = { suggestion ->
                        state.nameSuggestions
                            .firstOrNull { it.name == suggestion.name }
                            ?.let(viewModel::onNameSuggestion)
                    },
                    onFocusChange = viewModel::onNameFocus,
                )
                if (category != null && visual != null) {
                    CategorySelector(
                        name = category.name,
                        glyph = visual.glyph,
                        state =
                        if (state.categoryLocked) CategorySelectorState.Manual else CategorySelectorState.Auto,
                        onClick = { viewModel.onOpenSheet(ExpenseEditSheet.Category) },
                        letter = visual.letter,
                        sourceLabel = sourceLabel(
                            locked = state.categoryLocked,
                            suggestion = state.suggestion,
                            originalSource = state.originalSource,
                        ),
                        containerColor = visual.containerColor,
                    )
                }
                PlaceAutocomplete(
                    query = state.locationName,
                    onQueryChange = viewModel::onLocationChange,
                    suggestions =
                    if (state.locationFocused) {
                        state.locationSuggestions.map { location ->
                            PlaceSuggestion(
                                name = location.name,
                                detail = ExpenseFormat.locationDetail(location, today, zoneId),
                            )
                        }
                    } else {
                        emptyList()
                    },
                    onSuggestionClick = { suggestion ->
                        state.locationSuggestions
                            .firstOrNull { it.name == suggestion.name }
                            ?.let(viewModel::onLocationSuggestion)
                    },
                    onFocusChange = viewModel::onLocationFocus,
                )
                FormField(
                    label = stringResource(R.string.field_comment),
                    value = state.comment,
                    onValueChange = viewModel::onCommentChange,
                    kind = FormFieldKind.Text,
                )
                Spacer(Modifier.height(spacing.sm))
            }
            PrimaryButton(
                label =
                if (state.saving) {
                    stringResource(R.string.expense_saving)
                } else if (state.isEdit) {
                    stringResource(R.string.save_changes)
                } else {
                    stringResource(R.string.save)
                },
                onClick = { viewModel.onSave(onSaved) },
                enabled = viewModel.canSave(),
            )
            if (state.isEdit) {
                Spacer(Modifier.height(spacing.xs))
                PrimaryButton(
                    label = stringResource(R.string.delete_expense),
                    onClick = { viewModel.onOpenSheet(ExpenseEditSheet.DeleteConfirm) },
                    enabled = !state.deleting,
                    tone = ButtonTone.Destructive,
                )
            }
        }
        state.notice?.let { notice ->
            ExpenseToast(
                message =
                stringResource(
                    if (notice == ExpenseEditNotice.DeleteFailed) {
                        R.string.action_failed_delete
                    } else {
                        R.string.action_failed_save
                    },
                ),
                tone = ToastTone.Error,
                modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(spacing.md),
            )
        }
    }
    when (state.sheet) {
        ExpenseEditSheet.None -> Unit

        ExpenseEditSheet.Amount ->
            AmountSheet(
                amountInput = state.amountInput,
                onKey = viewModel::onAmountKey,
                onDismiss = viewModel::onDismissSheet,
            )

        ExpenseEditSheet.Date ->
            ExpenseDatePicker(
                selected = state.spentAt,
                today = today,
                onSelected = viewModel::onDateSelected,
                onDismiss = viewModel::onDismissSheet,
            )

        ExpenseEditSheet.Category ->
            CategoryPickerSheet(
                name = state.name,
                locationName = state.locationName,
                query = state.categoryQuery,
                selectedId = state.category?.id,
                categories = state.categories,
                suggestion = state.suggestion,
                onQueryChange = viewModel::onCategoryQueryChange,
                onSelect = viewModel::onSelectCategory,
                onCreate = { viewModel.onOpenSheet(ExpenseEditSheet.CreateCategory) },
                onDismiss = viewModel::onDismissSheet,
            )

        ExpenseEditSheet.CreateCategory ->
            CreateCategorySheet(
                name = state.createCategoryName,
                error = state.createCategoryError,
                saving = state.creatingCategory,
                onNameChange = viewModel::onCreateCategoryNameChange,
                onSave = viewModel::onCreateCategory,
                onDismiss = viewModel::onDismissSheet,
            )

        ExpenseEditSheet.DeleteConfirm ->
            DestructiveDialog(
                title = stringResource(R.string.delete_expense_title),
                description =
                stringResource(
                    R.string.delete_expense_description,
                    state.name,
                    amountFieldValue(state.amountInput).ifBlank { stringResource(R.string.amount_zero) },
                ),
                confirmLabel =
                if (state.deleting) {
                    stringResource(R.string.expense_deleting)
                } else {
                    stringResource(R.string.delete)
                },
                cancelLabel = stringResource(R.string.cancel),
                onConfirm = { viewModel.onConfirmDelete(onBack) },
                onDismiss = viewModel::onDismissSheet,
                confirmEnabled = !state.deleting,
            )
    }
}

@Composable
private fun AmountSheet(
    amountInput: String,
    onKey: (com.olegbelyanin.expensetracker.ui.components.KeypadKey) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ExpenseTheme.colors
    ExpenseBottomSheet(onDismiss = onDismiss, size = BottomSheetSize.Tall) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.amount_sheet_title),
                style = ExpenseTheme.typography.titleSection,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            TextAction(label = stringResource(R.string.close), onClick = onDismiss)
        }
        Text(
            text = amountFieldValue(amountInput).ifBlank { stringResource(R.string.amount_zero) },
            style = ExpenseTheme.typography.displayAmount,
            color = colors.textPrimary,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = ExpenseTheme.spacing.xs),
        )
        Keypad(onKey = onKey)
        Spacer(Modifier.height(ExpenseTheme.spacing.xs))
        PrimaryButton(label = stringResource(R.string.done), onClick = onDismiss)
    }
}

@Composable
private fun CreateCategorySheet(
    name: String,
    error: CategoryNameError?,
    saving: Boolean,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ExpenseBottomSheet(onDismiss = onDismiss, size = BottomSheetSize.Medium) {
        Text(
            text = stringResource(R.string.create_category_title),
            style = ExpenseTheme.typography.titleSection,
            color = ExpenseTheme.colors.textPrimary,
        )
        FormField(
            label = stringResource(R.string.field_category_name),
            value = name,
            onValueChange = onNameChange,
            error = error?.toMessage(),
        )
        PrimaryButton(
            label =
            if (saving) {
                stringResource(R.string.category_saving)
            } else {
                stringResource(R.string.create_category_save)
            },
            onClick = onSave,
            enabled = !saving && name.any { it.isLetterOrDigit() },
        )
    }
}

@Composable
private fun AmountFieldError.toMessage(): String = when (this) {
    AmountFieldError.EMPTY -> stringResource(R.string.amount_error_empty)
    AmountFieldError.ZERO -> stringResource(R.string.amount_error_zero)
    AmountFieldError.NEGATIVE -> stringResource(R.string.amount_error_negative)
    AmountFieldError.NON_NUMERIC -> stringResource(R.string.amount_error_non_numeric)
}

@Composable
private fun NameFieldError.toMessage(): String = when (this) {
    NameFieldError.EMPTY -> stringResource(R.string.name_error_empty)
}

@Composable
private fun CategoryNameError.toMessage(): String = when (this) {
    CategoryNameError.EMPTY -> stringResource(R.string.category_name_error_empty)
    CategoryNameError.DUPLICATE -> stringResource(R.string.category_name_error_duplicate)
}

@Composable
private fun sourceLabel(
    locked: Boolean,
    suggestion: CategorizationResult?,
    originalSource: CategoryAssignmentSource?,
): String = when (val caption = CategorySuggestionUi.caption(locked, suggestion, originalSource)) {
    CategorySourceCaption.Manual -> stringResource(R.string.category_source_manual)

    CategorySourceCaption.Fallback -> stringResource(R.string.category_source_auto)

    CategorySourceCaption.UserRule -> stringResource(R.string.category_source_rule)

    is CategorySourceCaption.Dictionary ->
        if (caption.confidence == null) {
            stringResource(R.string.category_source_dictionary_plain)
        } else {
            stringResource(R.string.category_source_dictionary, ExpenseFormat.scorePercent(caption.confidence))
        }

    is CategorySourceCaption.Place ->
        stringResource(R.string.category_source_place, ExpenseFormat.scorePercent(caption.confidence))
}

private fun amountFieldValue(raw: String): String {
    if (raw.isBlank()) return ""
    val digits = raw.replace(" ", "")
    val parse = com.olegbelyanin.expensetracker.domain.expense.ExpenseInputValidator().parseAmount(digits)
    return if (parse is com.olegbelyanin.expensetracker.domain.expense.AmountParseResult.Valid) {
        ExpenseFormat.money(parse.money.minor)
    } else {
        raw
    }
}
