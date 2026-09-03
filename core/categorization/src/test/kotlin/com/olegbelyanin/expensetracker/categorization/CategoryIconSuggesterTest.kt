package com.olegbelyanin.expensetracker.categorization

import com.olegbelyanin.expensetracker.model.CategoryIcons
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryIconSuggesterTest {
    private val suggester = CategoryIconSuggester()

    @Test
    fun suggestsPetsForFigmaExample() {
        assertEquals("pets", suggester.suggest("Питомцы"))
    }

    @Test
    fun suggestsBuiltinIconByName() {
        assertEquals("cafe", suggester.suggest("Кафе"))
        assertEquals("health", suggester.suggest("Стоматология"))
    }

    @Test
    fun fallsBackToLetter() {
        assertEquals(CategoryIcons.LETTER, suggester.suggest("Подписка"))
        assertEquals(CategoryIcons.LETTER, suggester.suggest("   "))
    }
}
