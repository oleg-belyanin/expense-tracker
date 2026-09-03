package com.olegbelyanin.expensetracker.domain

import com.olegbelyanin.expensetracker.model.Location
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    suspend fun suggest(query: String, limit: Int): List<Location>

    suspend fun findById(id: Long): Location?

    fun observeAll(): Flow<List<Location>>
}
