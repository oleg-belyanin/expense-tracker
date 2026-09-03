package com.olegbelyanin.expensetracker

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.data.theme.ThemeRepository
import com.olegbelyanin.expensetracker.database.AppDatabase
import com.olegbelyanin.expensetracker.database.RoomCategoryRepository
import com.olegbelyanin.expensetracker.database.RoomExpenseRepository
import com.olegbelyanin.expensetracker.database.RoomLocationRepository
import com.olegbelyanin.expensetracker.database.seed.SeedImporter
import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.LocationRepository
import com.olegbelyanin.expensetracker.domain.expense.DeleteExpenseUseCase
import com.olegbelyanin.expensetracker.domain.expense.ExpenseInputValidator
import com.olegbelyanin.expensetracker.domain.expense.SaveExpenseUseCase
import com.olegbelyanin.expensetracker.domain.expense.SuggestLocationsUseCase
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.ZoneId

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not provided")
}

class AppContainer(context: Context) {
    private val clock: Clock = Clock.systemDefaultZone()
    private val zoneId: ZoneId = ZoneId.systemDefault()

    val textNormalizer: TextNormalizer = TextNormalizer()
    val database: AppDatabase = AppDatabase.create(context.applicationContext)
    val categoryRepository: CategoryRepository = RoomCategoryRepository(database)
    val expenseRepository: ExpenseRepository = RoomExpenseRepository(database, textNormalizer, clock)
    val locationRepository: LocationRepository = RoomLocationRepository(database, textNormalizer)
    val saveExpense: SaveExpenseUseCase = SaveExpenseUseCase(
        expenses = expenseRepository,
        categories = categoryRepository,
        validator = ExpenseInputValidator(),
        clock = clock,
        zoneId = zoneId,
    )
    val deleteExpense: DeleteExpenseUseCase = DeleteExpenseUseCase(expenseRepository)
    val suggestLocations: SuggestLocationsUseCase = SuggestLocationsUseCase(locationRepository)
    val themeRepository: ThemeRepository = ThemeRepository(context)

    init {
        runBlocking {
            SeedImporter(
                database = database,
                assets = context.applicationContext.assets,
                normalizer = textNormalizer,
            ).importIfNeeded()
        }
    }
}
