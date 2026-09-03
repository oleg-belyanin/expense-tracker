package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

object SeedArtifactWriter {
    fun write(
        outputDir: Path,
        snapshot: SeedSnapshot,
        document: CategorizationConfigDocument,
        metrics: ValidationMetrics,
        trainRows: Int,
        validationRows: Int,
    ) {
        outputDir.createDirectories()
        val configJson = ConfigDocumentFormat.write(document)
        outputDir.resolve("categorization-config.json").writeText(configJson)
        outputDir.resolve("keyword_stats.json").writeText(writeKeywordStats(snapshot.keywordStats))
        outputDir.resolve("location_stats.json").writeText(writeLocationStats(snapshot.locationStats))
        outputDir.resolve("name_contexts.json").writeText(writeContexts(snapshot.contexts))
        outputDir.resolve("exact_rules.json").writeText(writeExactRules(snapshot.exactRules))
        outputDir.resolve("manifest.json").writeText(
            writeManifest(
                SeedManifestDto(
                    seedDataVersion = document.seedDataVersion,
                    normalizerVersion = document.normalizerVersion,
                    generatedAt = document.generatedAt,
                    trainRows = trainRows,
                    validationRows = validationRows,
                    validationTop1Accuracy = metrics.top1Accuracy,
                    keywordFeatures = snapshot.keywordStats.map { it.kind to it.keyword }.distinct().size,
                    locationFeatures = snapshot.locationStats.map { it.location }.distinct().size,
                ),
            ),
        )
    }

    fun writeConfig(path: Path, document: CategorizationConfigDocument) {
        path.parent?.createDirectories()
        path.writeText(ConfigDocumentFormat.write(document))
    }

    fun writeReport(
        path: Path,
        document: CategorizationConfigDocument,
        metrics: ValidationMetrics,
        snapshot: SeedSnapshot,
    ) {
        path.parent?.createDirectories()
        val engine = document.engine
        path.writeText(
            """
            |{
            |  "top1Accuracy": ${ratio(metrics.top1Accuracy)},
            |  "top3Recall": ${ratio(metrics.top3Recall)},
            |  "fallbackRate": ${ratio(metrics.fallbackRate)},
            |  "top1Hits": ${metrics.top1Hits},
            |  "top3Hits": ${metrics.top3Hits},
            |  "fallbackHits": ${metrics.fallbackHits},
            |  "rows": ${metrics.rows},
            |  "keywordFeatures": ${snapshot.keywordStats.map { it.kind to it.keyword }.distinct().size},
            |  "locationFeatures": ${snapshot.locationStats.map { it.location }.distinct().size},
            |  "exactRules": ${snapshot.exactRules.size},
            |  "nameContexts": ${snapshot.contexts.size},
            |  "params": {
            |    "MIN_SEED_SUPPORT": ${engine.minSeedSupport},
            |    "MIN_SEED_PROBABILITY": ${dec(engine.minSeedProbability)},
            |    "MAX_SEED_STRENGTH": ${dec(engine.maxSeedStrength)},
            |    "NAME_WEIGHT": ${dec(engine.nameWeight)},
            |    "LOCATION_WEIGHT": ${dec(engine.locationWeight)},
            |    "LAPLACE_ALPHA": ${dec(engine.laplaceAlpha)}
            |  }
            |}
            |
            """.trimMargin(),
        )
    }

    private fun writeKeywordStats(rows: List<SeedKeywordStatDto>): String = writeArray(rows) { row ->
        val keyword = quote(row.keyword)
        val kind = quote(row.kind)
        val code = quote(row.category_code)
        """{"keyword": $keyword, "kind": $kind, "category_code": $code, "count": ${row.count}}"""
    }

    private fun writeLocationStats(rows: List<SeedLocationStatDto>): String = writeArray(rows) { row ->
        val location = quote(row.location)
        val code = quote(row.category_code)
        """{"location": $location, "category_code": $code, "count": ${row.count}}"""
    }

    private fun writeContexts(rows: List<SeedNameContextDto>): String = writeArray(rows) { row ->
        val name = quote(row.normalized_name)
        val code = quote(row.category_code)
        val keywords = row.keywords.joinToString(", ") { quote(it) }
        """{"normalized_name": $name, "category_code": $code, "keywords": [$keywords]}"""
    }

    private fun writeExactRules(rows: List<SeedExactRuleDto>): String = writeArray(rows) { row ->
        """{"normalized_name": ${quote(row.normalized_name)}, "category_code": ${quote(row.category_code)}}"""
    }

    private fun writeManifest(manifest: SeedManifestDto): String {
        val accuracy = manifest.validationTop1Accuracy?.let { ratio(it) } ?: "null"
        return """
            |{
            |  "seedDataVersion": ${manifest.seedDataVersion},
            |  "normalizerVersion": ${manifest.normalizerVersion},
            |  "generatedAt": ${quote(manifest.generatedAt)},
            |  "trainRows": ${manifest.trainRows},
            |  "validationRows": ${manifest.validationRows},
            |  "validationTop1Accuracy": $accuracy,
            |  "keywordFeatures": ${manifest.keywordFeatures},
            |  "locationFeatures": ${manifest.locationFeatures}
            |}
            |
        """.trimMargin()
    }

    private fun <T> writeArray(rows: List<T>, encode: (T) -> String): String {
        if (rows.isEmpty()) return "[]\n"
        return rows.joinToString(",\n", "[\n", "\n]\n") { row -> "  " + encode(row) }
    }

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }

    private fun ratio(value: Double): String = String.format(Locale.US, "%.4f", value)

    private fun dec(value: Double): String = String.format(Locale.US, "%.2f", value)
}

object ConfigDocumentFormat {
    const val DEFAULT_GENERATED_AT = "2026-09-04"

    fun read(json: String): CategorizationConfigDocument = CategorizationConfigDocument(
        engine = CategorizationConfig.fromJson(json),
        seedDataVersion = CategorizationConfig.jsonInt(json, "SEED_DATA_VERSION") ?: 1,
        normalizerVersion = CategorizationConfig.jsonInt(json, "NORMALIZER_VERSION") ?: 1,
        generatedAt = CategorizationConfig.jsonString(json, "GENERATED_AT") ?: DEFAULT_GENERATED_AT,
    )

    fun write(document: CategorizationConfigDocument): String {
        val engine = document.engine
        return """
            |{
            |  "MIN_SEED_SUPPORT": ${engine.minSeedSupport},
            |  "MIN_SEED_PROBABILITY": ${String.format(Locale.US, "%.2f", engine.minSeedProbability)},
            |  "MAX_SEED_STRENGTH": ${formatNumber(engine.maxSeedStrength)},
            |  "NAME_WEIGHT": ${String.format(Locale.US, "%.1f", engine.nameWeight)},
            |  "LOCATION_WEIGHT": ${String.format(Locale.US, "%.1f", engine.locationWeight)},
            |  "LAPLACE_ALPHA": ${String.format(Locale.US, "%.1f", engine.laplaceAlpha)},
            |  "TRANSITION_MARGIN": ${String.format(Locale.US, "%.1f", engine.transitionMargin)},
            |  "TRANSITION_EPSILON": ${String.format(Locale.US, "%.1e", engine.transitionEpsilon)},
            |  "NORMALIZER_VERSION": ${document.normalizerVersion},
            |  "SEED_DATA_VERSION": ${document.seedDataVersion},
            |  "GENERATED_AT": "${document.generatedAt}"
            |}
            |
        """.trimMargin()
    }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else String.format(Locale.US, "%.1f", value)
}
