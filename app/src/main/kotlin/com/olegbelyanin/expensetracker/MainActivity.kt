package com.olegbelyanin.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olegbelyanin.expensetracker.data.theme.ThemePreference
import com.olegbelyanin.expensetracker.ui.navigation.ExpenseTrackerNav
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ExpenseTrackerApp).container
        setContent {
            val themePreference by container.themeRepository.theme.collectAsStateWithLifecycle(
                initialValue = ThemePreference.System,
            )
            CompositionLocalProvider(LocalAppContainer provides container) {
                ExpenseTrackerTheme(themePreference = themePreference) {
                    ExpenseTrackerNav(themePreference = themePreference)
                }
            }
        }
    }
}
