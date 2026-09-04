package com.olegbelyanin.expensetracker.ui.theme

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.olegbelyanin.expensetracker.data.theme.ThemePreference

private val LocalExpenseColors =
    staticCompositionLocalOf { LightExpenseColors }
private val LocalExpenseTypography =
    staticCompositionLocalOf { ExpenseTypographyTokens }

object ExpenseTheme {
    val colors: ExpenseColors
        @Composable get() = LocalExpenseColors.current

    val typography: ExpenseTypography
        @Composable get() = LocalExpenseTypography.current

    val spacing: ExpenseSpacing = ExpenseSpacingTokens
    val radii: ExpenseRadii = ExpenseRadiiTokens
}

@Composable
fun ExpenseTrackerTheme(themePreference: ThemePreference = ThemePreference.System, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark =
        when (themePreference) {
            ThemePreference.System -> systemDark
            ThemePreference.Light -> false
            ThemePreference.Dark -> true
        }
    val colors = if (dark) DarkExpenseColors else LightExpenseColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.setBackgroundDrawable(
                ColorDrawable(if (dark) Color.parseColor("#0D131A") else Color.parseColor("#F7F8F4")),
            )
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(
        LocalExpenseColors provides colors,
        LocalExpenseTypography provides ExpenseTypographyTokens,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = ExpenseTypographyTokens.toMaterialTypography(),
            content = content,
        )
    }
}

private fun ExpenseColors.toMaterialColorScheme(): ColorScheme {
    val scheme =
        if (isDark) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
    return scheme.copy(
        primary = action,
        onPrimary = onAction,
        secondary = action,
        onSecondary = onAction,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceSubtle,
        onSurfaceVariant = textSecondary,
        outline = border,
        error = danger,
        onError = onAction,
    )
}

private fun ExpenseTypography.toMaterialTypography(): Typography = Typography(
    headlineLarge = headlineScreen,
    headlineMedium = headlineScreen,
    titleLarge = titleSection,
    titleMedium = bodyPrimary,
    bodyLarge = bodyPrimary,
    bodyMedium = bodySecondary,
    labelLarge = labelControl,
    labelMedium = labelSmall,
    labelSmall = labelSmall,
    displaySmall = displayAmount,
)
