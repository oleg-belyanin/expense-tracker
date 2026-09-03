package com.olegbelyanin.expensetracker.ui.categories

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.ui.components.StatePanel
import com.olegbelyanin.expensetracker.ui.components.StatePanelType
import com.olegbelyanin.expensetracker.ui.navigation.AppTab
import com.olegbelyanin.expensetracker.ui.navigation.TabScaffold
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

@Composable
fun CategoriesScreen(onOpenSettings: () -> Unit, onTabSelected: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    TabScaffold(
        title = stringResource(R.string.tab_categories),
        selectedTab = AppTab.Categories,
        onTabSelected = onTabSelected,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        subtitle = {
            Text(
                text = stringResource(R.string.categories_empty_subtitle),
                style = typography.bodySecondary,
                color = colors.textSecondary,
            )
        },
    ) {
        StatePanel(
            type = StatePanelType.Empty,
            title = stringResource(R.string.categories_empty_title),
            description = stringResource(R.string.categories_empty_description),
            modifier = Modifier.padding(top = ExpenseTheme.spacing.sm),
        )
    }
}
