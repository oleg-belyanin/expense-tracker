package com.olegbelyanin.expensetracker.ui.navigation

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.olegbelyanin.expensetracker.LocalAppContainer
import com.olegbelyanin.expensetracker.data.theme.ThemePreference
import com.olegbelyanin.expensetracker.ui.analytics.AnalyticsScreen
import com.olegbelyanin.expensetracker.ui.categories.CategoriesScreen
import com.olegbelyanin.expensetracker.ui.categories.CategoriesViewModel
import com.olegbelyanin.expensetracker.ui.categories.CategoryFormScreen
import com.olegbelyanin.expensetracker.ui.categories.CategoryFormViewModel
import com.olegbelyanin.expensetracker.ui.expense.ExpenseEditScreen
import com.olegbelyanin.expensetracker.ui.expense.ExpenseEditViewModel
import com.olegbelyanin.expensetracker.ui.expenses.ExpensesScreen
import com.olegbelyanin.expensetracker.ui.expenses.ExpensesViewModel
import com.olegbelyanin.expensetracker.ui.settings.SettingsScreen

@Composable
fun ExpenseTrackerNav(themePreference: ThemePreference, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val expensesListState = rememberLazyListState()
    val expensesViewModel: ExpensesViewModel =
        viewModel(factory = container.expensesViewModelFactory())
    val categoriesViewModel: CategoriesViewModel =
        viewModel(factory = container.categoriesViewModelFactory())
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

    fun openExpense(id: String) {
        backStack.add(AppDestination.ExpenseEdit(expenseId = id))
    }

    fun openCategoryForm(categoryId: Long? = null) {
        backStack.add(AppDestination.CategoryForm(categoryId = categoryId))
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
                                    viewModel = expensesViewModel,
                                    listState = expensesListState,
                                    onOpenSettings = ::openSettings,
                                    onAddExpense = ::openNewExpense,
                                    onOpenExpense = ::openExpense,
                                    onTabSelected = ::selectTab,
                                )

                            AppTab.Analytics ->
                                AnalyticsScreen(
                                    onOpenSettings = ::openSettings,
                                    onTabSelected = ::selectTab,
                                )

                            AppTab.Categories ->
                                CategoriesScreen(
                                    viewModel = categoriesViewModel,
                                    onOpenSettings = ::openSettings,
                                    onTabSelected = ::selectTab,
                                    onCreateCategory = { openCategoryForm() },
                                    onEditCategory = { openCategoryForm(it) },
                                )
                        }
                    }

                is AppDestination.ExpenseEdit ->
                    NavEntry(key) {
                        val editViewModel: ExpenseEditViewModel =
                            viewModel(
                                key = key.sessionId,
                                factory = container.expenseEditViewModelFactory(key.expenseId),
                            )
                        ExpenseEditScreen(
                            viewModel = editViewModel,
                            onBack = ::pop,
                            onSaved = { savedId ->
                                expensesViewModel.onExpenseSaved(savedId)
                                pop()
                            },
                        )
                    }

                is AppDestination.CategoryForm ->
                    NavEntry(key) {
                        val formViewModel: CategoryFormViewModel =
                            viewModel(
                                key = key.sessionId,
                                factory = container.categoryFormViewModelFactory(key.categoryId),
                            )
                        CategoryFormScreen(
                            viewModel = formViewModel,
                            onBack = ::pop,
                            onSaved = ::pop,
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
