package com.olegbelyanin.expensetracker.categorization

import com.olegbelyanin.expensetracker.model.KeywordKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {
    private val normalizer = TextNormalizer()

    @Test
    fun mapsYoLowercaseAndTrim() {
        assertEquals("жилье", normalizer.normalizePlain("  Жильё  "))
        assertEquals("латте", normalizer.analyze("Латте").normalizedName)
    }

    @Test
    fun keepsQuotedPhraseAsSingleFeature() {
        val result = normalizer.analyze("Кетостерил в \"Столичке на Чкалова\"")
        assertTrue(result.features.any { it.kind == KeywordKind.PHRASE && it.value.contains("столичке") })
        assertTrue(result.features.any { it.value == "кетостерил" })
    }
}
