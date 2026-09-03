package com.olegbelyanin.expensetracker.database.search

object LikeQuery {
    fun escape(raw: String): String = buildString(raw.length) {
        for (ch in raw) {
            when (ch) {
                '\\', '%', '_' -> {
                    append('\\')
                    append(ch)
                }

                else -> append(ch)
            }
        }
    }

    fun prefix(raw: String): String = "${escape(raw)}%"

    fun contains(raw: String): String = "%${escape(raw)}%"
}

object FtsQuery {
    fun prefixMatch(normalizedQuery: String): String? {
        val tokens = TOKEN_REGEX.findAll(normalizedQuery).map { it.value }.toList()
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { token ->
            "\"${token.replace("\"", "\"\"")}\"*"
        }
    }

    private val TOKEN_REGEX = Regex("[\\p{L}\\p{N}]+")
}
