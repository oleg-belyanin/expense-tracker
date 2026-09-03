package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.model.BuiltinCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.readText

class GoldenSeedTest {
    private val catalog = CategoryCatalog.builtin()
    private val train = DatasetReader.read(ProjectRoot.dir().resolve("seed-data/raw/train.csv").toPath(), catalog)
    private val validation =
        DatasetReader.read(ProjectRoot.dir().resolve("seed-data/raw/validation.csv").toPath(), catalog)
    private val config = loadFrozenConfig()

    @Test
    fun goldenRowsFromSeedPlanClassifyToExpectedCategories() {
        val (snapshot, _) = SeedPipeline.build(train, validation, catalog, config)
        val seedCatalog = SeedCatalog(snapshot, catalog, config)
        SeedPipeline.goldenRows.forEach { row ->
            val result = seedCatalog.categorize(row.name, row.location)
            assertEquals(
                "${row.name} / ${row.location}",
                row.categoryCode,
                catalog.code(result.selectedCategoryId),
            )
        }
    }

    @Test
    fun seedContextsIncludeDoctorAndDentistForTransitions() {
        val contexts = ContextBuilder.build(train, TextNormalizer())
        val byName = contexts.associateBy { it.normalized_name }
        assertEquals("HEALTH", byName.getValue("врач").category_code)
        assertEquals(listOf("врач"), byName.getValue("врач").keywords)
        assertEquals("HEALTH", byName.getValue("стоматолог").category_code)
        assertEquals(listOf("стоматолог"), byName.getValue("стоматолог").keywords)
    }

    @Test
    fun validationMeetsSeedPlanTargets() {
        val (_, metrics) = SeedPipeline.build(train, validation, catalog, config)
        assertTrue(
            "top-1 ${metrics.top1Accuracy} < ${ValidationRunner.TARGET_TOP1}",
            metrics.top1Accuracy >= ValidationRunner.TARGET_TOP1,
        )
        assertTrue(
            "fallback ${metrics.fallbackRate} >= ${ValidationRunner.TARGET_FALLBACK}",
            metrics.fallbackRate < ValidationRunner.TARGET_FALLBACK,
        )
        assertEquals(200, metrics.rows)
        assertEquals(BuiltinCategories.all.size * 20, validation.size)
    }

    @Test
    fun configRoundTripKeepsGridParams() {
        val json = ConfigDocumentFormat.write(
            CategorizationConfigDocument(
                config,
                seedDataVersion = 1,
                normalizerVersion = 1,
                generatedAt = "2026-09-04",
            ),
        )
        val parsed = ConfigDocumentFormat.read(json).engine
        assertEquals(config.minSeedSupport, parsed.minSeedSupport)
        assertEquals(config.minSeedProbability, parsed.minSeedProbability, 1e-9)
        assertEquals(config.maxSeedStrength, parsed.maxSeedStrength, 1e-9)
        assertEquals(config.nameWeight, parsed.nameWeight, 1e-9)
        assertEquals(config.locationWeight, parsed.locationWeight, 1e-9)
        assertEquals(config.laplaceAlpha, parsed.laplaceAlpha, 1e-9)
    }

    private fun loadFrozenConfig(): CategorizationConfig {
        val path = ProjectRoot.dir().resolve("seed-data/categorization-config.json")
        return CategorizationConfig.fromJson(path.readText())
    }
}
