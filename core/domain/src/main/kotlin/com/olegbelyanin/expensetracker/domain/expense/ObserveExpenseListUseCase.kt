package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class ObserveExpenseListUseCase(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val locations: LocationRepository,
    private val clock: Clock,
    private val zoneId: ZoneId,
) {
    fun observe(filter: ExpenseListFilter): Flow<ExpenseListSlice> {
        val today = LocalDate.now(clock.withZone(zoneId))
        return combine(
            expenses.observeMatching(ExpenseSearchQuery.from(filter, today, zoneId)),
            expenses.observeCount(),
            categories.observeAll(),
            locations.observeAll(),
        ) { expenseRows, storedCount, categoryRows, locationRows ->
            ExpenseListAssembler.build(
                expenses = expenseRows,
                categories = categoryRows,
                locations = locationRows,
                filter = filter,
                today = today,
                zoneId = zoneId,
                storedCount = storedCount,
            )
        }
    }
}
