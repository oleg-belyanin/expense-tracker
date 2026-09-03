package com.olegbelyanin.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.olegbelyanin.expensetracker.data.theme.ThemePreference
import com.olegbelyanin.expensetracker.ui.analytics.AnalyticsScreen
import com.olegbelyanin.expensetracker.ui.categories.CategoriesScreen
import com.olegbelyanin.expensetracker.ui.expense.ExpenseEditScreen
import com.olegbelyanin.expensetracker.ui.expenses.ExpensesScreen
import com.olegbelyanin.expensetracker.ui.settings.SettingsScreen

@Composable
fun ExpenseTrackerNav(themePreference: ThemePreference, modifier: Modifier = Modifier) {
    val backStack =
        remember {
            mutableStateListOf<AppDestination>(AppDestination.Tab(AppTab.Expenses))
        }

    fun selectTab(tab: AppTab) {
        backStack.clear()
        backStack.add(AppDestination.Tab(tab))
    }

    fun openSettings() {
        if (backStack.lastOrNull() !is AppDestination.Settings) {
            backStack.add(AppDestination.Settings)
        }
    }

    fun openNewExpense() {
        backStack.add(AppDestination.ExpenseEdit())
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = ::pop,
        entryProvider = { key ->
            when (key) {
                is AppDestination.Tab ->
                    NavEntry(key) {
                        when (key.tab) {
                            AppTab.Expenses ->
                                ExpensesScreen(
                                    onOpenSettings = ::openSettings,
                                    onAddExpense = ::openNewExpense,
                                    onTabSelected = ::selectTab,
                                )

                            AppTab.Analytics ->
                                AnalyticsScreen(
                                    onOpenSettings = ::openSettings,
                                    onTabSelected = ::selectTab,
                                )

                            AppTab.Categories ->
                                CategoriesScreen(
                                    onOpenSettings = ::openSettings,
                                    onTabSelected = ::selectTab,
                                )
                        }
                    }

                is AppDestination.ExpenseEdit ->
                    NavEntry(key) {
                        ExpenseEditScreen(
                            expenseId = key.expenseId,
                            onBack = ::pop,
                        )
                    }

                AppDestination.Settings ->
                    NavEntry(key) {
                        SettingsScreen(
                            themePreference = themePreference,
                            onBack = ::pop,
                        )
                    }
            }
        },
    )
}
