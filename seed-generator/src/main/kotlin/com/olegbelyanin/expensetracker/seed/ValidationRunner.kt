package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.CategorizationConfig

object ValidationRunner {
    const val TARGET_TOP1 = 0.85
    const val TARGET_FALLBACK = 0.15

    val supportGrid = listOf(1, 2, 3, 4, 5)
    val probabilityGrid = listOf(0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85)
    val strengthGrid = listOf(10.0, 20.0, 50.0, 100.0)
    val nameWeightGrid = listOf(1.0, 1.5, 2.0, 2.5, 3.0)
    val locationWeightGrid = listOf(0.5, 1.0, 1.5, 2.0)
    val alphaGrid = listOf(0.1, 0.25, 0.5, 1.0)

    fun evaluate(
        rows: List<DatasetRow>,
        snapshot: SeedSnapshot,
        catalog: CategoryCatalog,
        config: CategorizationConfig,
    ): ValidationMetrics {
        val seedCatalog = SeedCatalog(snapshot, catalog, config)
        var top1 = 0
        var top3 = 0
        var fallback = 0
        rows.forEach { row ->
            val result = seedCatalog.categorize(row.name, row.location)
            val expectedId = catalog.id(row.categoryCode)
            if (result.selectedCategoryId == expectedId) top1++
            if (result.orderedCandidates.take(3).any { it.categoryId == expectedId }) top3++
            if (result.usedFallback) fallback++
        }
        return ValidationMetrics(rows.size, top1, top3, fallback)
    }

    fun mistakes(
        rows: List<DatasetRow>,
        snapshot: SeedSnapshot,
        catalog: CategoryCatalog,
        config: CategorizationConfig,
    ): List<String> {
        val seedCatalog = SeedCatalog(snapshot, catalog, config)
        return rows.mapNotNull { row ->
            val result = seedCatalog.categorize(row.name, row.location)
            val predicted = catalog.code(result.selectedCategoryId)
            if (predicted == row.categoryCode) {
                null
            } else {
                val top = result.orderedCandidates.take(3).joinToString { catalog.code(it.categoryId) }
                "${row.name} / ${row.location ?: "—"} expected=${row.categoryCode} got=$predicted top3=[$top]"
            }
        }
    }

    fun search(
        stats: TrainStatistics,
        contexts: List<SeedNameContextDto>,
        rows: List<DatasetRow>,
        catalog: CategoryCatalog,
        base: CategorizationConfig,
    ): Pair<CategorizationConfig, ValidationMetrics> {
        var bestConfig: CategorizationConfig? = null
        var bestMetrics: ValidationMetrics? = null
        supportGrid.forEach { support ->
            probabilityGrid.forEach { probability ->
                val filterConfig = base.copy(minSeedSupport = support, minSeedProbability = probability)
                val snapshot = SeedPipeline.artifact(stats, contexts, filterConfig)
                strengthGrid.forEach { strength ->
                    alphaGrid.forEach { alpha ->
                        nameWeightGrid.forEach { nameWeight ->
                            locationWeightGrid.forEach { locationWeight ->
                                val config = filterConfig.copy(
                                    maxSeedStrength = strength,
                                    laplaceAlpha = alpha,
                                    nameWeight = nameWeight,
                                    locationWeight = locationWeight,
                                )
                                val metrics = evaluate(rows, snapshot, catalog, config)
                                if (isBetter(metrics, config, bestMetrics, bestConfig)) {
                                    bestConfig = config
                                    bestMetrics = metrics
                                }
                            }
                        }
                    }
                }
            }
        }
        val chosen = checkNotNull(bestConfig) { "Grid search produced no configuration" }
        val metrics = checkNotNull(bestMetrics)
        return chosen to metrics
    }

    fun meetsTargets(metrics: ValidationMetrics): Boolean =
        metrics.top1Accuracy >= TARGET_TOP1 && metrics.fallbackRate < TARGET_FALLBACK

    private fun isBetter(
        metrics: ValidationMetrics,
        config: CategorizationConfig,
        bestMetrics: ValidationMetrics?,
        bestConfig: CategorizationConfig?,
    ): Boolean {
        if (bestMetrics == null || bestConfig == null) return true
        val candidateOk = meetsTargets(metrics)
        val bestOk = meetsTargets(bestMetrics)
        if (candidateOk != bestOk) return candidateOk
        if (metrics.top1Hits != bestMetrics.top1Hits) return metrics.top1Hits > bestMetrics.top1Hits
        if (metrics.top3Hits != bestMetrics.top3Hits) return metrics.top3Hits > bestMetrics.top3Hits
        if (metrics.fallbackHits != bestMetrics.fallbackHits) return metrics.fallbackHits < bestMetrics.fallbackHits
        return compareConfigs(config, bestConfig) < 0
    }

    private fun compareConfigs(left: CategorizationConfig, right: CategorizationConfig): Int = compareValuesBy(
        left,
        right,
        { it.minSeedSupport },
        { it.minSeedProbability },
        { it.maxSeedStrength },
        { it.nameWeight },
        { it.locationWeight },
        { it.laplaceAlpha },
    )
}
