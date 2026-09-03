package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class ObserveAnalyticsUseCase(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val clock: Clock,
    private val zoneId: ZoneId,
) {
    fun observe(filter: ExpenseListFilter): Flow<AnalyticsSlice> = combine(
        expenses.observeAll(),
        categories.observeAll(),
    ) { expenseRows, categoryRows ->
        AnalyticsAssembler.build(
            expenses = expenseRows,
            categories = categoryRows,
            filter = filter,
            today = LocalDate.now(clock.withZone(zoneId)),
            zoneId = zoneId,
        )
    }
}
