package com.olegbelyanin.expensetracker.database

import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.search.FtsQuery
import com.olegbelyanin.expensetracker.database.search.LikeQuery
import com.olegbelyanin.expensetracker.database.search.LocationSuggestionRanker
import com.olegbelyanin.expensetracker.domain.LocationRepository
import com.olegbelyanin.expensetracker.model.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomLocationRepository(private val database: AppDatabase, private val normalizer: TextNormalizer) :
    LocationRepository {
    override fun observeAll(): Flow<List<Location>> =
        database.locationDao().observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: Long): Location? = database.locationDao().findById(id)?.toDomain()

    override suspend fun suggest(query: String, limit: Int): List<Location> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return database.locationDao().recentUsed(limit).map { it.toDomain() }
        }
        val normalized = normalizer.normalizePlain(trimmed)
        val prefixHits = database.locationDao()
            .suggestByPrefix(LikeQuery.prefix(normalized), LikeQuery.prefix(trimmed), limit)
            .map { it.toDomain() }
        val match = FtsQuery.prefixMatch(normalized)
        val ftsHits = if (match == null) {
            emptyList()
        } else {
            runCatching { database.locationFtsDao().search(match) }
                .getOrDefault(emptyList())
                .map { it.toDomain() }
        }
        return LocationSuggestionRanker.merge(normalized, prefixHits, ftsHits, limit)
    }
}
