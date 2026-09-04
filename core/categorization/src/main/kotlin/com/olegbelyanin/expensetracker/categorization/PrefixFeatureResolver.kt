package com.olegbelyanin.expensetracker.categorization

/**
 * Раннее сопоставление неполного токена со словарём: «кет» → «кетостер»,
 * если все подходящие слова указывают на одну категорию.
 */
object PrefixFeatureResolver {
    const val MIN_LENGTH = 2
    const val LOOKUP_CHARS = 3

    fun lookupStem(typed: String): String? {
        if (typed.length < MIN_LENGTH) return null
        return typed.take(LOOKUP_CHARS)
    }

    fun matches(typed: String, keyword: String): Boolean =
        typed.isNotEmpty() && (keyword.startsWith(typed) || typed.startsWith(keyword))

    fun <T> choose(typed: String, candidates: List<PrefixCandidate<T>>): PrefixCandidate<T>? {
        if (typed.length < MIN_LENGTH) return null
        val matches = candidates.filter { matches(typed, it.value) }
        if (matches.isEmpty()) return null
        val exact = matches.find { it.value == typed }
        if (exact != null) return exact
        if (matches.map { it.topCategoryId }.distinct().size != 1) return null
        return matches.maxWith(
            compareBy<PrefixCandidate<T>> { it.value.length }
                .thenBy { it.support }
                .thenBy { it.value },
        )
    }
}

data class PrefixCandidate<T>(val value: String, val topCategoryId: Long, val support: Int, val payload: T)
