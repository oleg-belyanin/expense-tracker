package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.ui.theme.ExpenseColors
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

@Composable
fun CategoryAvatar(
    glyph: CategoryGlyphKey,
    containerColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    letter: String = "",
) {
    val glyphSize = size * 0.5f
    Box(
        modifier =
        modifier
            .size(size)
            .clip(ExpenseTheme.radii.full)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        CategoryGlyph(
            key = glyph,
            size = glyphSize,
            letter = letter,
        )
    }
}

fun ExpenseColors.colorForGlyph(key: CategoryGlyphKey): Color = when (key) {
    CategoryGlyphKey.Groceries, CategoryGlyphKey.Shopping -> groceries
    CategoryGlyphKey.Cafe -> cafe
    CategoryGlyphKey.Transport, CategoryGlyphKey.Sports -> transport
    CategoryGlyphKey.Health -> health
    CategoryGlyphKey.Housing, CategoryGlyphKey.Education -> housing
    CategoryGlyphKey.Comms, CategoryGlyphKey.Travel -> comms
    CategoryGlyphKey.Fun, CategoryGlyphKey.Pets -> funColor
    CategoryGlyphKey.Clothes -> clothes
    CategoryGlyphKey.Home, CategoryGlyphKey.Work -> home
    CategoryGlyphKey.Other, CategoryGlyphKey.Letter -> other
}
