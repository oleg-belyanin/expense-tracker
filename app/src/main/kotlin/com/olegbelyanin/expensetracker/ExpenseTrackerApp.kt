package com.olegbelyanin.expensetracker

import android.app.Application

class ExpenseTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
