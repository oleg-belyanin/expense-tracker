package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

enum class BottomSheetSize {
    Compact,
    Medium,
    Tall,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    size: BottomSheetSize = BottomSheetSize.Compact,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ExpenseTheme.colors
    val spacing = ExpenseTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxHeight =
        when (size) {
            BottomSheetSize.Compact -> 240.dp
            BottomSheetSize.Medium -> 360.dp
            BottomSheetSize.Tall -> 640.dp
        }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = ExpenseTheme.radii.lg,
        dragHandle = null,
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            content = content,
        )
    }
}
