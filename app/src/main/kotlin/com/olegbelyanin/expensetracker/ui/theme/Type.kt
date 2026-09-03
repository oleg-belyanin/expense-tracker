package com.olegbelyanin.expensetracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.olegbelyanin.expensetracker.R

val GolosText =
    FontFamily(
        Font(R.font.golos_text_regular, FontWeight.Normal),
        Font(R.font.golos_text_medium, FontWeight.Medium),
        Font(R.font.golos_text_semibold, FontWeight.SemiBold),
        Font(R.font.golos_text_bold, FontWeight.Bold),
    )

@Immutable
data class ExpenseTypography(
    val headlineScreen: TextStyle,
    val displayAmount: TextStyle,
    val titleSection: TextStyle,
    val bodyPrimary: TextStyle,
    val bodySecondary: TextStyle,
    val labelControl: TextStyle,
    val labelSmall: TextStyle,
)

internal val ExpenseTypographyTokens =
    ExpenseTypography(
        headlineScreen =
        TextStyle(
            fontFamily = GolosText,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.3).sp,
        ),
        displayAmount =
        TextStyle(
            fontFamily = GolosText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.4).sp,
        ),
        titleSection =
        TextStyle(
            fontFamily = GolosText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyPrimary =
        TextStyle(
            fontFamily = GolosText,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodySecondary =
        TextStyle(
            fontFamily = GolosText,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelControl =
        TextStyle(
            fontFamily = GolosText,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelSmall =
        TextStyle(
            fontFamily = GolosText,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp,
        ),
    )
