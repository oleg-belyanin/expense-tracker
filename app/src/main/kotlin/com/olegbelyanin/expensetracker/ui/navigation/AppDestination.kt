package com.olegbelyanin.expensetracker.ui.navigation

enum class AppTab {
    Expenses,
    Analytics,
    Categories,
}

sealed interface AppDestination {
    data class Tab(val tab: AppTab) : AppDestination

    data class ExpenseEdit(
        val expenseId: String? = null,
        val sessionId: String = java.util.UUID.randomUUID().toString(),
    ) : AppDestination

    data object Settings : AppDestination
}
