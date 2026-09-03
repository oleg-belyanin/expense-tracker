package com.olegbelyanin.expensetracker.domain

import com.olegbelyanin.expensetracker.model.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

interface LocationRepository {
    suspend fun suggest(query: String, limit: Int): List<Location>

    suspend fun findById(id: Long): Location?

    fun observeAll(): Flow<List<Location>>

    fun observeUsed(): Flow<List<Location>> = observeAll().map { rows ->
        rows.filter { it.archivedAt == null && it.usageCount > 0 }
            .sortedWith(
                compareByDescending<Location> { it.lastUsedAt ?: Instant.EPOCH }
                    .thenByDescending { it.usageCount }
                    .thenBy { it.id },
            )
    }
}
