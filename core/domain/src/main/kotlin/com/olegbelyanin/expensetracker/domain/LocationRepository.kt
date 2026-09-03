package com.olegbelyanin.expensetracker.domain

import com.olegbelyanin.expensetracker.model.Location

interface LocationRepository {
    suspend fun suggest(query: String, limit: Int): List<Location>
}
