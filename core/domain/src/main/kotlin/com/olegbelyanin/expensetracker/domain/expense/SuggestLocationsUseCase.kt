package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.LocationRepository
import com.olegbelyanin.expensetracker.model.Location

class SuggestLocationsUseCase(private val locations: LocationRepository) {
    suspend operator fun invoke(query: String, limit: Int = DEFAULT_LIMIT): List<Location> =
        locations.suggest(query, limit)

    companion object {
        const val DEFAULT_LIMIT = 8
    }
}
