package com.olegbelyanin.expensetracker

sealed interface AppStartup {
    data object Loading : AppStartup

    data object Ready : AppStartup

    data object Failed : AppStartup
}
