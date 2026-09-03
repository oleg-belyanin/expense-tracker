package com.olegbelyanin.expensetracker.database.seed

/** §15 AD-CAT-001: seed-обновление трогает только строки `source=seed`. */
object SeedImportRules {
    const val SOURCE_SEED = "seed"

    fun shouldReplaceExactRule(existingSource: String?): Boolean =
        existingSource == null || existingSource == SOURCE_SEED

    fun shouldReplaceNameContext(existingSource: String?): Boolean =
        existingSource == null || existingSource == SOURCE_SEED
}
