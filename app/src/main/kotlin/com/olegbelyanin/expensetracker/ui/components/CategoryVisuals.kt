package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.ui.graphics.Color
import com.olegbelyanin.expensetracker.domain.expense.AnalyticsCategoryRow
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.ui.theme.ExpenseColors

data class CategoryVisual(val glyph: CategoryGlyphKey, val containerColor: Color, val letter: String)

fun AnalyticsCategoryRow.toVisual(colors: ExpenseColors): CategoryVisual {
    val glyph = CategoryGlyphKey.fromStorage(icon)
    val stored = parseHexColor(color)
    val container =
        when {
            !isBuiltin && stored != null -> stored
            glyph == CategoryGlyphKey.Letter -> stored ?: colors.other
            else -> colors.colorForGlyph(glyph)
        }
    return CategoryVisual(glyph = glyph, containerColor = container, letter = name)
}

fun Category.toVisual(colors: ExpenseColors): CategoryVisual {
    val glyph = CategoryGlyphKey.fromStorage(icon)
    val stored = parseHexColor(color)
    val container =
        when {
            !isBuiltin && stored != null -> stored
            glyph == CategoryGlyphKey.Letter -> stored ?: colors.other
            else -> colors.colorForGlyph(glyph)
        }
    return CategoryVisual(glyph = glyph, containerColor = container, letter = name)
}

fun parseHexColor(raw: String): Color? {
    val hex = raw.removePrefix("#")
    val value = hex.toLongOrNull(16) ?: return null
    return when (hex.length) {
        6 -> Color(value or 0xFF000000)
        8 -> Color(value)
        else -> null
    }
}
