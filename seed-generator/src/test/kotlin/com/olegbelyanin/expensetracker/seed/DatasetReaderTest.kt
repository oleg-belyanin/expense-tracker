package com.olegbelyanin.expensetracker.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files

class DatasetReaderTest {
    private val catalog = CategoryCatalog.builtin()

    @Test
    fun parseCsvLineUnescapesQuotedPhrase() {
        val fields = DatasetReader.parseCsvLine("\"\"\"молоко 3.2%\"\"\",Перекрёсток,GROCERIES")
        assertEquals(listOf("\"молоко 3.2%\"", "Перекрёсток", "GROCERIES"), fields)
    }

    @Test
    fun readsEmptyLocationAsNull() {
        val path = Files.createTempFile("train", ".csv")
        Files.writeString(path, "name,location,category_code\nХлеб,,GROCERIES\n")
        val rows = DatasetReader.read(path, catalog)
        assertEquals(1, rows.size)
        assertEquals("Хлеб", rows.single().name)
        assertNull(rows.single().location)
        assertEquals("GROCERIES", rows.single().categoryCode)
    }

    @Test
    fun categoriesYamlMatchesBuiltinCodes() {
        val fromYaml = CategoriesReader.read(
            ProjectRoot.dir().resolve("seed-data/categories.yaml").toPath(),
        )
        assertEquals(catalog.idsByCode, fromYaml.idsByCode)
        assertEquals(catalog.fallbackCode, fromYaml.fallbackCode)
    }

    @Test
    fun trainCsvHasExpectedVolumeAndGoldens() {
        val path = ProjectRoot.dir().resolve("seed-data/raw/train.csv").toPath()
        val rows = DatasetReader.read(path, catalog)
        assertEquals(800, rows.size)
        SeedPipeline.goldenRows.forEach { golden ->
            assertEquals(
                1,
                rows.count {
                    it.name == golden.name && it.location == golden.location && it.categoryCode == golden.categoryCode
                },
            )
        }
    }
}
