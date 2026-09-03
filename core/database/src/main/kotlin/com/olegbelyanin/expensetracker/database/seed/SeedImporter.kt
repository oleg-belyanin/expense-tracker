package com.olegbelyanin.expensetracker.database.seed

import android.content.res.AssetManager
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.AppDatabase
import com.olegbelyanin.expensetracker.database.entities.AppMetaEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryEntity
import com.olegbelyanin.expensetracker.database.entities.ExactCategoryRuleEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LocationCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.LocationEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextKeywordEntity
import com.olegbelyanin.expensetracker.database.learning.CategoryNameExperienceWriter
import com.olegbelyanin.expensetracker.model.BuiltinCategories
import com.olegbelyanin.expensetracker.model.KeywordKind
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
        insertBuiltinCategoriesIfEmpty()
        importSeedRows(manifest)
        database.metaDao().put(
            AppMetaEntity(SEED_DATA_VERSION_KEY, manifest.seedDataVersion.toString()),
        )
        database.metaDao().put(
            AppMetaEntity(NORMALIZER_VERSION_KEY, manifest.normalizerVersion.toString()),
        )
    }

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

    private suspend fun importSeedRows(manifest: SeedManifestDto) {
        val keywordStats = json.decodeFromString<List<SeedKeywordStatDto>>(
            assets.readText("seed/keyword_stats.json"),
        )
        val locationStats = json.decodeFromString<List<SeedLocationStatDto>>(
            assets.readText("seed/location_stats.json"),
        )
        val contexts = json.decodeFromString<List<SeedNameContextDto>>(
            assets.readText("seed/name_contexts.json"),
        )
        val exactRules = optionalJsonList<SeedExactRuleDto>("seed/exact_rules.json")
        keywordStats.forEach { row ->
            val category = requireCategory(row.category_code)
            val kind = KeywordKind.valueOf(row.kind.uppercase())
            val keywordId = requireKeyword(kind, normalizer.normalizePlain(row.keyword))
            database.seedDao().upsertKeywordStat(
                KeywordCategoryStatEntity(
                    keywordId = keywordId,
                    categoryId = category.id,
                    source = SOURCE_SEED,
                    observationCount = row.count,
                ),
            )
        }
        locationStats.forEach { row ->
            val category = requireCategory(row.category_code)
            val locationId = requireSeedLocation(row.location)
            database.seedDao().upsertLocationStat(
                LocationCategoryStatEntity(
                    locationId = locationId,
                    categoryId = category.id,
                    source = SOURCE_SEED,
                    observationCount = row.count,
                ),
            )
        }
        val now = Instant.now().toEpochMilli()
        contexts.forEach { row ->
            val category = requireCategory(row.category_code)
            database.seedDao().upsertNameContext(
                NameCategoryContextEntity(
                    normalizedName = row.normalized_name,
                    categoryId = category.id,
                    source = SOURCE_SEED,
                    updatedAt = now,
                ),
            )
            row.keywords.forEach { keyword ->
                val keywordId = requireKeyword(KeywordKind.WORD, normalizer.normalizePlain(keyword))
                database.seedDao().upsertNameContextKeyword(
                    NameCategoryContextKeywordEntity(
                        normalizedName = row.normalized_name,
                        keywordId = keywordId,
                    ),
                )
            }
        }
        exactRules.forEach { row ->
            val category = requireCategory(row.category_code)
            database.seedDao().upsertExactRule(
                ExactCategoryRuleEntity(
                    normalizedName = row.normalized_name,
                    categoryId = category.id,
                    source = SOURCE_SEED,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    private suspend fun requireCategory(code: String) = database.categoryDao().findBuiltinByCode(code)
        ?: error("Unknown seed category_code=$code")

    private suspend fun requireKeyword(kind: KeywordKind, value: String): Long {
        val existing = database.keywordDao().find(kind.name.lowercase(), value)
        if (existing != null) return existing.id
        return database.keywordDao().insert(
            KeywordEntity(value = value, kind = kind.name.lowercase()),
        )
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
        const val SOURCE_SEED = "seed"
    }
}
