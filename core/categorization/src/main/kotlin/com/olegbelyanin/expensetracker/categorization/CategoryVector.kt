package com.olegbelyanin.expensetracker.categorization

data class FeatureCount(val categoryId: Long, val source: String, val count: Int)

data class CategoryVector(val scores: Map<Long, Double>) {
    fun score(categoryId: Long): Double = scores[categoryId] ?: 0.0

    fun argMax(): Long? = scores.entries
        .sortedWith(compareByDescending<Map.Entry<Long, Double>> { it.value }.thenBy { it.key })
        .firstOrNull()
        ?.key

    fun renormalize(): CategoryVector {
        val total = scores.values.sum()
        if (total <= 0.0) return this
        return CategoryVector(scores.mapValues { (_, value) -> value / total })
    }

    fun applyTransition(fromCategoryId: Long, toCategoryId: Long, margin: Double, epsilon: Double): CategoryVector {
        val p0 = score(fromCategoryId)
        val p1 = score(toCategoryId)
        val multiplier = margin + p0 / maxOf(p1, epsilon)
        val next = scores.toMutableMap()
        next[fromCategoryId] = p0
        next[toCategoryId] = multiplier * p1
        return CategoryVector(next).renormalize()
    }

    companion object {
        const val SOURCE_SEED = "seed"
        const val SOURCE_USER = "user"
        const val SOURCE_CATEGORY_NAME = "category_name"

        fun average(vectors: List<CategoryVector>): CategoryVector? {
            if (vectors.isEmpty()) return null
            val ids = vectors.flatMap { it.scores.keys }.toSet()
            val size = vectors.size.toDouble()
            return CategoryVector(ids.associateWith { id -> vectors.sumOf { it.score(id) } / size })
        }

        fun combine(
            name: CategoryVector?,
            location: CategoryVector?,
            nameWeight: Double,
            locationWeight: Double,
        ): CategoryVector? {
            if (name == null) return location
            if (location == null) return name
            val ids = name.scores.keys + location.scores.keys
            val denom = nameWeight + locationWeight
            if (denom <= 0.0) return name
            return CategoryVector(
                ids.associateWith { id ->
                    (nameWeight * name.score(id) + locationWeight * location.score(id)) / denom
                },
            )
        }

        fun fromCounts(
            counts: List<FeatureCount>,
            activeCategoryIds: Set<Long>,
            config: CategorizationConfig,
        ): CategoryVector? {
            if (activeCategoryIds.isEmpty()) return null
            val relevant = counts.filter { it.categoryId in activeCategoryIds && it.count > 0 }
            if (relevant.isEmpty()) return null
            val seed = mutableMapOf<Long, Double>()
            val user = mutableMapOf<Long, Double>()
            val categoryName = mutableMapOf<Long, Double>()
            relevant.forEach { row ->
                val target = when (row.source) {
                    SOURCE_SEED -> seed
                    SOURCE_CATEGORY_NAME -> categoryName
                    else -> user
                }
                target[row.categoryId] = (target[row.categoryId] ?: 0.0) + row.count
            }
            val seedSupport = seed.values.sum()
            val seedStrength = minOf(seedSupport, config.maxSeedStrength)
            val effective = activeCategoryIds.associateWith { id ->
                val seedPart = if (seedSupport > 0.0) seedStrength * (seed[id] ?: 0.0) / seedSupport else 0.0
                seedPart + (user[id] ?: 0.0) + (categoryName[id] ?: 0.0)
            }
            val alpha = config.laplaceAlpha
            val denom = effective.values.sum() + alpha * activeCategoryIds.size
            return CategoryVector(
                activeCategoryIds.associateWith { id -> (effective.getValue(id) + alpha) / denom },
            )
        }

        fun locationEligible(counts: List<FeatureCount>, config: CategorizationConfig): Boolean {
            val support = counts.sumOf { it.count }
            if (support < config.minSeedSupport) return false
            val maxShare = counts.maxOf { it.count }.toDouble() / support
            return maxShare >= config.minSeedProbability
        }

        /**
         * Ровно две категории с одинаковым сырым счётчиком — ничья 50/50,
         * без сглаживания Лапласа.
         */
        fun evenTwoWaySplit(counts: List<FeatureCount>): Set<Long>? {
            val byCategory = counts
                .filter { it.count > 0 }
                .groupBy { it.categoryId }
                .mapValues { (_, rows) -> rows.sumOf { it.count } }
                .filter { it.value > 0 }
            if (byCategory.size != 2) return null
            val totals = byCategory.values.toList()
            if (totals[0] != totals[1]) return null
            return byCategory.keys
        }
    }
}
