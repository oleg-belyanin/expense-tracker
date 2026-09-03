package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

sealed interface KeypadKey {
    data class Digit(val value: Char) : KeypadKey

    data object Comma : KeypadKey

    data object Backspace : KeypadKey
}

@Composable
fun Keypad(onKey: (KeypadKey) -> Unit, modifier: Modifier = Modifier) {
    val spacing = ExpenseTheme.spacing
    val rows =
        listOf(
            listOf(KeypadKey.Digit('1'), KeypadKey.Digit('2'), KeypadKey.Digit('3')),
            listOf(KeypadKey.Digit('4'), KeypadKey.Digit('5'), KeypadKey.Digit('6')),
            listOf(KeypadKey.Digit('7'), KeypadKey.Digit('8'), KeypadKey.Digit('9')),
            listOf(KeypadKey.Comma, KeypadKey.Digit('0'), KeypadKey.Backspace),
        )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                row.forEach { key ->
                    KeypadButton(
                        key = key,
                        onClick = { onKey(key) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(key: KeypadKey, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ExpenseTheme.colors
    val isBackspace = key is KeypadKey.Backspace
    val background = if (isBackspace) colors.surfaceSubtle else colors.surface
    val content = if (isBackspace) colors.action else colors.textPrimary
    val label =
        when (key) {
            is KeypadKey.Digit -> key.value.toString()
            KeypadKey.Comma -> ","
            KeypadKey.Backspace -> stringResource(R.string.keypad_backspace)
        }
    Box(
        modifier =
        modifier
            .height(56.dp)
            .clip(ExpenseTheme.radii.full)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ExpenseTheme.typography.titleSection,
            color = content,
        )
    }
}
