package com.olegbelyanin.expensetracker.ui.categories

import com.olegbelyanin.expensetracker.domain.category.CategoryNameError
import com.olegbelyanin.expensetracker.model.CategoryIcons
import com.olegbelyanin.expensetracker.model.CategoryPalette

data class CategoryFormDraft(
    val name: String = "",
    val color: String = CategoryPalette.swatches.first(),
    val icon: String = CategoryIcons.LETTER,
    val iconManual: Boolean = false,
    val colorManual: Boolean = false,
    val nameTouched: Boolean = false,
    val attemptedSave: Boolean = false,
) {
    fun withName(value: String, suggestedIcon: String): CategoryFormDraft {
        val nextIcon = if (iconManual) icon else CategoryIcons.canonicalize(suggestedIcon)
        val nextColor = if (colorManual) color else CategoryPalette.swatchForIcon(nextIcon)
        return copy(name = value, icon = nextIcon, color = nextColor, nameTouched = true)
    }

    fun withColor(value: String): CategoryFormDraft = copy(color = value, colorManual = true)

    fun withIcon(value: String): CategoryFormDraft {
        val nextIcon = CategoryIcons.canonicalize(value)
        val nextColor = if (colorManual) color else CategoryPalette.swatchForIcon(nextIcon)
        return copy(icon = nextIcon, iconManual = true, color = nextColor)
    }

    fun markAttempted(): CategoryFormDraft = copy(attemptedSave = true)

    fun nameError(duplicate: Boolean): CategoryNameError? {
        val empty = name.none { it.isLetterOrDigit() }
        if (empty) {
            return if (nameTouched || attemptedSave) CategoryNameError.EMPTY else null
        }
        return if (duplicate) CategoryNameError.DUPLICATE else null
    }

    fun canSave(duplicate: Boolean): Boolean = name.any { it.isLetterOrDigit() } && !duplicate
}
