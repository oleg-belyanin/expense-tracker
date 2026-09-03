package com.olegbelyanin.expensetracker.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ExpenseSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
)

@Immutable
data class ExpenseRadii(
    val md: RoundedCornerShape = RoundedCornerShape(12.dp),
    val lg: RoundedCornerShape = RoundedCornerShape(16.dp),
    val full: RoundedCornerShape = CircleShape,
)

internal val ExpenseSpacingTokens = ExpenseSpacing()
internal val ExpenseRadiiTokens = ExpenseRadii()

val MinTapTarget = 48.dp
val BottomNavHeight = 72.dp
val FabSize = 56.dp
