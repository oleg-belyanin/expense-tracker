package com.olegbelyanin.expensetracker.categorization

/**
 * Одна версионированная конфигурация категоризации (§21 AD-CAT-001).
 * Значения после grid search совпадают с `seed-data/categorization-config.json`.
 */
data class CategorizationConfig(
    val laplaceAlpha: Double = 0.1,
    val maxSeedStrength: Double = 10.0,
    val nameWeight: Double = 1.0,
    val locationWeight: Double = 1.0,
    val minSeedSupport: Int = 1,
    val minSeedProbability: Double = 0.55,
    val transitionMargin: Double = 0.1,
    val transitionEpsilon: Double = 1e-9,
) {
    companion object {
        val DEFAULT = CategorizationConfig()

        fun fromJson(json: String): CategorizationConfig = CategorizationConfig(
            laplaceAlpha = jsonNumber(json, "LAPLACE_ALPHA") ?: DEFAULT.laplaceAlpha,
            maxSeedStrength = jsonNumber(json, "MAX_SEED_STRENGTH") ?: DEFAULT.maxSeedStrength,
            nameWeight = jsonNumber(json, "NAME_WEIGHT") ?: DEFAULT.nameWeight,
            locationWeight = jsonNumber(json, "LOCATION_WEIGHT") ?: DEFAULT.locationWeight,
            minSeedSupport = jsonNumber(json, "MIN_SEED_SUPPORT")?.toInt() ?: DEFAULT.minSeedSupport,
            minSeedProbability = jsonNumber(json, "MIN_SEED_PROBABILITY") ?: DEFAULT.minSeedProbability,
            transitionMargin = jsonNumber(json, "TRANSITION_MARGIN") ?: DEFAULT.transitionMargin,
            transitionEpsilon = jsonNumber(json, "TRANSITION_EPSILON") ?: DEFAULT.transitionEpsilon,
        )

        fun jsonNumber(json: String, key: String): Double? {
            val match = Regex(""""$key"\s*:\s*([-+0-9.eE]+)""").find(json) ?: return null
            return match.groupValues[1].toDouble()
        }

        fun jsonString(json: String, key: String): String? {
            val match = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""").find(json) ?: return null
            return match.groupValues[1]
        }

        fun jsonInt(json: String, key: String): Int? = jsonNumber(json, key)?.toInt()
    }
}
