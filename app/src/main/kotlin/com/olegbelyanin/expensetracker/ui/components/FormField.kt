package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

enum class FormFieldKind {
    Amount,
    Text,
    Date,
}

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    kind: FormFieldKind = FormFieldKind.Text,
    error: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val hasError = error != null
    val labelColor = if (hasError) colors.danger else colors.textSecondary
    val borderColor = if (hasError) colors.danger else colors.border
    val readOnly = kind != FormFieldKind.Text || onClick != null
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = if (hasError) 88.dp else 68.dp)
            .clip(ExpenseTheme.radii.md)
            .background(colors.surface)
            .border(1.dp, borderColor, ExpenseTheme.radii.md)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Text(
            text = label,
            style = typography.labelSmall,
            color = labelColor,
        )
        if (readOnly) {
            Text(
                text = value,
                style = typography.bodyPrimary,
                color = colors.textPrimary,
            )
        } else {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = typography.bodyPrimary.copy(color = colors.textPrimary),
                singleLine = true,
                cursorBrush = SolidColor(colors.action),
                keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            )
        }
        if (error != null) {
            FieldError(message = error)
        }
    }
}
