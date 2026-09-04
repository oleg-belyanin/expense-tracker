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
        assertEquals("латт", normalizer.analyze("Латте").normalizedName)
    }

    @Test
    fun collapsesRepeatedSpaces() {
        assertEquals("хлеб молоко", normalizer.normalizePlain("хлеб   молоко"))
    }

    @Test
    fun appliesNfkcToFullwidthLatinAndDigits() {
        assertEquals("ikea 123", normalizer.normalizePlain("ＩＫＥＡ １２３"))
    }

    @Test
    fun dropsPunctuationAndKeepsNumbers() {
        val result = normalizer.analyze("Хлеб!!! 5 кг")
        assertTrue(result.features.any { it.value == "хлеб" })
        assertTrue(result.features.any { it.value == "5" })
        assertTrue(result.features.any { it.value == "кг" })
    }

    @Test
    fun keepsLatinBrandsUnstemmed() {
        val result = normalizer.analyze("IKEA полка")
        assertTrue(result.features.any { it.kind == KeywordKind.WORD && it.value == "ikea" })
        assertTrue(result.features.any { it.value == "полк" })
    }

    @Test
    fun plainTokensSkipStemmingForPrefixSearch() {
        assertEquals(listOf("кетостерил"), normalizer.plainTokens("Кетостерил"))
        assertEquals(listOf("кет"), normalizer.plainTokens("Кет"))
        assertEquals("кетостер", normalizer.analyze("Кетостерил").normalizedName)
    }

    @Test
    fun stemsOrdinaryCyrillicWords() {
        val result = normalizer.analyze("Стоматология и ортодонтия")
        assertEquals("стоматолог ортодонт", result.normalizedName)
        assertEquals(
            setOf("стоматолог", "ортодонт"),
            result.features.filter { it.kind == KeywordKind.WORD }.map { it.value }.toSet(),
        )
    }

    @Test
    fun stemsDoctorFormsToSameFeature() {
        assertEquals("врач", normalizer.analyze("врач").normalizedName)
        assertEquals("врач", normalizer.analyze("Врачи").normalizedName)
        assertEquals("стоматолог", normalizer.analyze("стоматолог").normalizedName)
        assertEquals("стоматолог", normalizer.analyze("Стоматология").normalizedName)
    }

    @Test
    fun keepsQuotedPhraseAsSingleUnstemmedFeature() {
        val quoted = listOf(
            "Кетостерил в \"Столичке на Чкалова\"",
            "Кетостерил в «Столичке на Чкалова»",
            "Кетостерил в “Столичке на Чкалова”",
        )
        quoted.forEach { raw ->
            val result = normalizer.analyze(raw)
            assertEquals("кетостер \"столичке на чкалова\"", result.normalizedName)
            assertTrue(result.features.any { it.kind == KeywordKind.WORD && it.value == "кетостер" })
            assertTrue(
                result.features.any { it.kind == KeywordKind.PHRASE && it.value == "столичке на чкалова" },
            )
            assertTrue(result.features.none { it.value.contains("столичк") && it.kind == KeywordKind.WORD })
        }
    }

    @Test
    fun preservesPhraseThenWordOrder() {
        val result = normalizer.analyze("\"Ашан\" молоко")
        assertEquals("\"ашан\" молок", result.normalizedName)
    }

    @Test
    fun dropsStopWordsAndDeduplicatesFeatures() {
        val result = normalizer.analyze("Кафе и кафе")
        assertEquals("каф каф", result.normalizedName)
        assertEquals(1, result.features.size)
        assertEquals(KeywordFeature("каф", KeywordKind.WORD), result.features.single())
    }

    @Test
    fun goldenSeedNamesNormalizeAsSpecified() {
        GoldenNormalizationCases.all.forEach { case ->
            assertEquals(case.rawName, case.normalizedName, normalizer.analyze(case.rawName).normalizedName)
            val location = case.rawLocation ?: return@forEach
            assertEquals(location, case.normalizedLocation, normalizer.normalizePlain(location))
        }
    }

    @Test
    fun honorsStemExceptions() {
        val custom = TextNormalizer(stemExceptions = setOf("латте"))
        assertEquals("латте", custom.analyze("Латте").normalizedName)
    }
}
