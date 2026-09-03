package com.olegbelyanin.expensetracker.ui.expenses

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
fun ExpensesScreen(
    onOpenSettings: () -> Unit,
    onAddExpense: () -> Unit,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    TabScaffold(
        title = stringResource(R.string.tab_expenses),
        selectedTab = AppTab.Expenses,
        onTabSelected = onTabSelected,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        subtitle = {
            Text(
                text = stringResource(R.string.period_all_time),
                style = typography.labelSmall,
                color = colors.textSecondary,
            )
            Text(
                text = stringResource(R.string.amount_zero),
                style = typography.displayAmount,
                color = colors.textPrimary,
            )
        },
        showFab = true,
        onFabClick = onAddExpense,
    ) {
        StatePanel(
            type = StatePanelType.Empty,
            title = stringResource(R.string.expenses_empty_title),
            description = stringResource(R.string.expenses_empty_description),
            actionLabel = stringResource(R.string.add_expense),
            onAction = onAddExpense,
            modifier = Modifier.padding(top = ExpenseTheme.spacing.sm),
        )
    }
}
