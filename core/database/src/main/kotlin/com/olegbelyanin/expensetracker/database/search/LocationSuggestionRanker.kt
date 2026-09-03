package com.olegbelyanin.expensetracker.database.search

import com.olegbelyanin.expensetracker.model.Location
import java.time.Instant

object LocationSuggestionRanker {
    fun merge(
        queryNormalized: String,
        prefixHits: List<Location>,
        ftsHits: List<Location>,
        limit: Int,
    ): List<Location> {
        val byId = LinkedHashMap<Long, Location>()
        prefixHits.forEach { byId.putIfAbsent(it.id, it) }
        ftsHits.forEach { byId.putIfAbsent(it.id, it) }
        return byId.values
            .sortedWith(
                compareBy<Location> { matchQuality(queryNormalized, it) }
                    .thenByDescending { it.lastUsedAt ?: Instant.EPOCH }
                    .thenByDescending { it.usageCount }
                    .thenBy { it.id },
            )
            .take(limit)
    }

    private fun matchQuality(queryNormalized: String, location: Location): Int {
        val normalized = location.normalizedName
        return when {
            normalized == queryNormalized -> 0
            normalized.startsWith(queryNormalized) -> 1
            location.name.startsWith(queryNormalized, ignoreCase = true) -> 1
            else -> 2
        }
    }
}
