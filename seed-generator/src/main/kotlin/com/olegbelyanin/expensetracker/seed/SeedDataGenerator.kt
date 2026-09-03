package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val parsed = GeneratorArgs.parse(args)
    val result = SeedDataGenerator.generate(parsed)
    if (!ValidationRunner.meetsTargets(result.metrics)) {
        System.err.println(
            "Seed validation missed targets: top-1=${result.metrics.top1Accuracy} " +
                "fallback=${result.metrics.fallbackRate}",
        )
        result.mistakes.take(40).forEach { System.err.println("  $it") }
        exitProcess(1)
    }
}

object SeedDataGenerator {
    fun generate(args: GeneratorArgs): SeedBuildResult {
        val catalog = CategoriesReader.read(args.categories)
        val train = DatasetReader.read(args.train, catalog)
        val validation = DatasetReader.read(args.validation, catalog)
        SeedPipeline.validateDataset(train, validation, catalog)
        val existing = if (args.config.exists()) ConfigDocumentFormat.read(args.config.readText()) else null
        val normalizer = TextNormalizer()
        val stats = CounterBuilder.build(train, normalizer)
        val contexts = ContextBuilder.build(train, normalizer)
        println("Grid search on ${validation.size} validation rows...")
        val (config, metrics) = ValidationRunner.search(
            stats = stats,
            contexts = contexts,
            rows = validation,
            catalog = catalog,
            base = existing?.engine ?: CategorizationConfig.DEFAULT,
        )
        val snapshot = SeedPipeline.artifact(stats, contexts, config)
        val document = CategorizationConfigDocument(
            engine = config,
            seedDataVersion = existing?.seedDataVersion ?: 1,
            normalizerVersion = TextNormalizer.VERSION,
            generatedAt = existing?.generatedAt ?: ConfigDocumentFormat.DEFAULT_GENERATED_AT,
        )
        SeedArtifactWriter.write(
            outputDir = args.output,
            snapshot = snapshot,
            document = document,
            metrics = metrics,
            trainRows = train.size,
            validationRows = validation.size,
        )
        SeedArtifactWriter.writeConfig(args.config, document)
        args.report?.let { SeedArtifactWriter.writeReport(it, document, metrics, snapshot) }
        println(
            "Selected MIN_SEED_SUPPORT=${config.minSeedSupport} " +
                "MIN_SEED_PROBABILITY=${config.minSeedProbability} " +
                "MAX_SEED_STRENGTH=${config.maxSeedStrength} " +
                "NAME_WEIGHT=${config.nameWeight} " +
                "LOCATION_WEIGHT=${config.locationWeight} " +
                "LAPLACE_ALPHA=${config.laplaceAlpha}",
        )
        println(
            "Validation top-1=${metrics.top1Accuracy} top-3=${metrics.top3Recall} " +
                "fallback=${metrics.fallbackRate}",
        )
        return SeedBuildResult(
            snapshot = snapshot,
            config = document,
            metrics = metrics,
            mistakes = ValidationRunner.mistakes(validation, snapshot, catalog, config),
        )
    }
}

data class GeneratorArgs(
    val train: Path,
    val validation: Path,
    val output: Path,
    val config: Path,
    val categories: Path,
    val report: Path? = null,
) {
    companion object {
        fun parse(args: Array<String>): GeneratorArgs {
            val values = linkedMapOf<String, String>()
            var index = 0
            while (index < args.size) {
                val key = args[index]
                require(key.startsWith("--") && index + 1 < args.size) { "Usage: $USAGE" }
                values[key] = args[index + 1]
                index += 2
            }
            return GeneratorArgs(
                train = required(values, "--train"),
                validation = required(values, "--validation"),
                output = required(values, "--output"),
                config = required(values, "--config"),
                categories = required(values, "--categories"),
                report = values["--report"]?.let(Path::of),
            )
        }

        private fun required(values: Map<String, String>, key: String): Path =
            Path.of(values[key] ?: error("Missing $key. $USAGE"))

        private const val USAGE =
            "SeedDataGenerator --train FILE --validation FILE --output DIR --config FILE --categories FILE [--report FILE]"
    }
}
