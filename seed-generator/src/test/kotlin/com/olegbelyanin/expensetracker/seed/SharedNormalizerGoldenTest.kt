package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.GoldenNormalizationCases
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedNormalizerGoldenTest {
    private val normalizer = TextNormalizer()

    @Test
    fun seedGeneratorMatchesAppGoldenNormalizationContract() {
        GoldenNormalizationCases.all.forEach { case ->
            assertEquals(case.rawName, case.normalizedName, normalizer.analyze(case.rawName).normalizedName)
            val location = case.rawLocation ?: return@forEach
            assertEquals(location, case.normalizedLocation, normalizer.normalizePlain(location))
        }
    }
}
