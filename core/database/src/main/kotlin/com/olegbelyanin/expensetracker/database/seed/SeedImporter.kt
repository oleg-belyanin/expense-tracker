package com.olegbelyanin.expensetracker.database.seed

import android.content.res.AssetManager
import androidx.room3.withWriteTransaction
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.AppDatabase
import com.olegbelyanin.expensetracker.database.entities.AppMetaEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryEntity
import com.olegbelyanin.expensetracker.database.entities.LocationEntity
import com.olegbelyanin.expensetracker.database.learning.CategoryNameExperienceWriter
import com.olegbelyanin.expensetracker.model.BuiltinCategories
import kotlinx.serialization.json.Json
import java.time.Instant

class SeedImporter(
    private val database: AppDatabase,
    private val assets: AssetManager,
    private val normalizer: TextNormalizer,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun importIfNeeded() {
        val manifest = json.decodeFromString<SeedManifestDto>(assets.readText("seed/manifest.json"))
        val storedVersion = database.metaDao().get(SEED_DATA_VERSION_KEY)
        if (storedVersion == manifest.seedDataVersion.toString() &&
            database.categoryDao().count() > 0
        ) {
            return
        }
        val snapshot = loadSnapshot()
        val now = Instant.now().toEpochMilli()
        database.withWriteTransaction {
            insertBuiltinCategoriesIfEmpty()
            SeedWriter(
                learningDao = database.learningDao(),
                keywordDao = database.keywordDao(),
                normalizer = normalizer,
                requireCategoryId = { code ->
                    database.categoryDao().findBuiltinByCode(code)?.id
                        ?: error("Unknown seed category_code=$code")
                },
                requireLocationId = ::requireSeedLocation,
                activeCategoryIds = { database.categoryDao().getActive().map { it.id }.toSet() },
            ).apply(snapshot, now)
            database.metaDao().put(
                AppMetaEntity(SEED_DATA_VERSION_KEY, manifest.seedDataVersion.toString()),
            )
            database.metaDao().put(
                AppMetaEntity(NORMALIZER_VERSION_KEY, manifest.normalizerVersion.toString()),
            )
        }
    }

    private fun loadSnapshot(): SeedSnapshot = SeedSnapshot(
        keywordStats = json.decodeFromString(assets.readText("seed/keyword_stats.json")),
        locationStats = json.decodeFromString(assets.readText("seed/location_stats.json")),
        contexts = json.decodeFromString(assets.readText("seed/name_contexts.json")),
        exactRules = optionalJsonList("seed/exact_rules.json"),
    )

    private suspend fun insertBuiltinCategoriesIfEmpty() {
        if (database.categoryDao().count() > 0) return
        val now = Instant.now().toEpochMilli()
        val experience = CategoryNameExperienceWriter(database, normalizer)
        BuiltinCategories.all.forEach { spec ->
            val id = database.categoryDao().insert(
                CategoryEntity(
                    code = spec.code,
                    name = spec.name,
                    normalizedName = normalizer.analyze(spec.name).normalizedName,
                    color = spec.color,
                    icon = spec.icon,
                    isBuiltin = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            experience.writeIfMissing(id, spec.name)
        }
    }

    /**
     * Строка location нужна только как FK для `location_category_stat`.
     * usage_count = 0 — autocomplete (B1) такие места не показывает.
     */
    private suspend fun requireSeedLocation(rawLocation: String): Long {
        val normalized = normalizer.normalizePlain(rawLocation)
        val existing = database.locationDao().findByNormalizedName(normalized)
        if (existing != null) return existing.id
        val now = Instant.now().toEpochMilli()
        return database.locationDao().insert(
            LocationEntity(
                name = rawLocation,
                normalizedName = normalized,
                usageCount = 0,
                lastUsedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private inline fun <reified T> optionalJsonList(path: String): List<T> =
        runCatching { json.decodeFromString<List<T>>(assets.readText(path)) }
            .getOrDefault(emptyList())

    private fun AssetManager.readText(path: String): String = open(path).bufferedReader().use { it.readText() }

    companion object {
        const val SEED_DATA_VERSION_KEY = "seed_data_version"
        const val NORMALIZER_VERSION_KEY = "normalizer_version"
    }
}
