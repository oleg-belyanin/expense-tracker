package com.olegbelyanin.expensetracker.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinCategoriesTest {
    @Test
    fun tenBuiltinCategoriesAndProtectedFallback() {
        assertEquals(10, BuiltinCategories.all.size)
        assertEquals(1, BuiltinCategories.all.count { it.isFallback })
        assertEquals("OTHER", BuiltinCategories.fallback.code)
        assertEquals("Прочее", BuiltinCategories.fallback.name)
        assertTrue(BuiltinCategories.all.all { it.code.isNotBlank() && it.icon.isNotBlank() })
    }
}
