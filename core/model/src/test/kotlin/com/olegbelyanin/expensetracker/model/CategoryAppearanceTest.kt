package com.olegbelyanin.expensetracker.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryAppearanceTest {
    @Test
    fun userPaletteHasSevenSwatches() {
        assertEquals(7, CategoryPalette.swatches.size)
        assertTrue(CategoryPalette.swatches.all { it.startsWith("#") && it.length == 7 })
    }

    @Test
    fun unknownIconBecomesLetter() {
        assertEquals(CategoryIcons.LETTER, CategoryIcons.canonicalize("unknown"))
        assertEquals("comms", CategoryIcons.canonicalize("communication"))
        assertEquals("pets", CategoryIcons.canonicalize("pets"))
    }

    @Test
    fun petsIconMapsToPurpleSwatch() {
        assertEquals("#A76CC1", CategoryPalette.swatchForIcon("pets"))
        assertEquals(CategoryPalette.swatches.first(), CategoryPalette.swatchForIcon("letter"))
    }
}
