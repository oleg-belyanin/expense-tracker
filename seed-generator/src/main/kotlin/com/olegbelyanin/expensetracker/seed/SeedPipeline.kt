package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import com.olegbelyanin.expensetracker.categorization.TextNormalizer

object SeedPipeline {
    const val EXPECTED_TRAIN_ROWS = 800
    const val EXPECTED_VALIDATION_ROWS = 200
    const val ROWS_PER_TRAIN_CATEGORY = 80
    const val ROWS_PER_VALIDATION_CATEGORY = 20

    val goldenRows: List<DatasetRow> = listOf(
        DatasetRow("Латте", "Шоколадница", "CAFE"),
        DatasetRow("Кетостерил", "Столичка на Чкалова", "HEALTH"),
        DatasetRow("Бензин", "Лукойл", "TRANSPORT"),
        DatasetRow("Хлеб", null, "GROCERIES"),
        DatasetRow("Непонятная покупка", null, "OTHER"),
        DatasetRow("Врач", "Поликлиника", "HEALTH"),
        DatasetRow("Стоматолог", "Стоматология", "HEALTH"),
    )

    fun artifact(
        stats: TrainStatistics,
        contexts: List<SeedNameContextDto>,
        config: CategorizationConfig,
    ): SeedSnapshot {
        val filtered = SeedFilter.apply(stats, config)
        return filtered.copy(
            contexts = contexts.sortedBy { it.normalized_name },
            exactRules = filtered.exactRules,
        )
    }

    fun build(
        train: List<DatasetRow>,
        validation: List<DatasetRow>,
        catalog: CategoryCatalog,
        config: CategorizationConfig,
        normalizer: TextNormalizer = TextNormalizer(),
    ): Pair<SeedSnapshot, ValidationMetrics> {
        validateDataset(train, validation, catalog)
        val stats = CounterBuilder.build(train, normalizer)
        val contexts = ContextBuilder.build(train, normalizer)
        val snapshot = artifact(stats, contexts, config)
        return snapshot to ValidationRunner.evaluate(validation, snapshot, catalog, config)
    }

    fun validateDataset(train: List<DatasetRow>, validation: List<DatasetRow>, catalog: CategoryCatalog) {
        require(train.size == EXPECTED_TRAIN_ROWS) {
            "train.csv must have $EXPECTED_TRAIN_ROWS rows, got ${train.size}"
        }
        require(validation.size == EXPECTED_VALIDATION_ROWS) {
            "validation.csv must have $EXPECTED_VALIDATION_ROWS rows, got ${validation.size}"
        }
        catalog.idsByCode.keys.forEach { code ->
            val trainCount = train.count { it.categoryCode == code }
            val validationCount = validation.count { it.categoryCode == code }
            require(trainCount == ROWS_PER_TRAIN_CATEGORY) {
                "train $code must have $ROWS_PER_TRAIN_CATEGORY rows, got $trainCount"
            }
            require(validationCount == ROWS_PER_VALIDATION_CATEGORY) {
                "validation $code must have $ROWS_PER_VALIDATION_CATEGORY rows, got $validationCount"
            }
        }
        goldenRows.forEach { golden ->
            require(
                train.any {
                    it.name == golden.name && it.location == golden.location &&
                        it.categoryCode == golden.categoryCode
                },
            ) {
                "Missing golden train row: ${golden.name} / ${golden.location} / ${golden.categoryCode}"
            }
        }
    }
}
