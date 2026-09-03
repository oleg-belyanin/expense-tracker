package com.olegbelyanin.expensetracker.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.olegbelyanin.expensetracker.ui.components.BottomNav
import com.olegbelyanin.expensetracker.ui.components.ExpenseFab
import com.olegbelyanin.expensetracker.ui.components.SettingsAction
import com.olegbelyanin.expensetracker.ui.theme.BottomNavHeight
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

@Composable
fun TabScaffold(
    title: String,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable ColumnScope.() -> Unit)? = null,
    showFab: Boolean = false,
    onFabClick: () -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ExpenseTheme.colors
    val spacing = ExpenseTheme.spacing
    Box(
        modifier =
        modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.md, end = spacing.xs, top = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = ExpenseTheme.typography.headlineScreen,
                        color = colors.textPrimary,
                    )
                    subtitle?.invoke(this)
                }
                SettingsAction(onClick = onOpenSettings)
            }
            Column(
                modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md),
                content = content,
            )
            BottomNav(
                selected = selectedTab,
                onSelect = onTabSelected,
            )
        }
        if (showFab) {
            ExpenseFab(
                onClick = onFabClick,
                modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = spacing.md, bottom = BottomNavHeight + spacing.md),
            )
        }
        overlay()
    }
}
