package com.olegbelyanin.expensetracker

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.olegbelyanin.expensetracker.categorization.CategorizationConfig
import com.olegbelyanin.expensetracker.categorization.CategorizationEngine
import com.olegbelyanin.expensetracker.categorization.CategoryIconSuggester
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.data.theme.ThemeRepository
import com.olegbelyanin.expensetracker.database.AppDatabase
import com.olegbelyanin.expensetracker.database.RoomCategorizationCatalog
import com.olegbelyanin.expensetracker.database.RoomCategoryRepository
import com.olegbelyanin.expensetracker.database.RoomExpenseRepository
import com.olegbelyanin.expensetracker.database.RoomLearningRepository
import com.olegbelyanin.expensetracker.database.RoomLocationRepository
import com.olegbelyanin.expensetracker.database.seed.SeedImporter
import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.LearningRepository
import com.olegbelyanin.expensetracker.domain.LocationRepository
import com.olegbelyanin.expensetracker.domain.category.ArchiveCategoryUseCase
import com.olegbelyanin.expensetracker.domain.category.CreateCategoryUseCase
import com.olegbelyanin.expensetracker.domain.category.RestoreCategoryUseCase
import com.olegbelyanin.expensetracker.domain.category.UpdateCategoryUseCase
import com.olegbelyanin.expensetracker.domain.expense.ClearExpenseHistoryUseCase
import com.olegbelyanin.expensetracker.domain.expense.DeleteExpenseUseCase
import com.olegbelyanin.expensetracker.domain.expense.ExpenseInputValidator
import com.olegbelyanin.expensetracker.domain.expense.ImportExpensesUseCase
import com.olegbelyanin.expensetracker.domain.expense.ObserveAnalyticsUseCase
import com.olegbelyanin.expensetracker.domain.expense.ObserveExpenseListUseCase
import com.olegbelyanin.expensetracker.domain.expense.RecalculateCategoriesUseCase
import com.olegbelyanin.expensetracker.domain.expense.SaveExpenseUseCase
import com.olegbelyanin.expensetracker.domain.expense.SuggestCategoryUseCase
import com.olegbelyanin.expensetracker.domain.expense.SuggestLocationsUseCase
import com.olegbelyanin.expensetracker.domain.learning.ObserveRememberedRuleCountUseCase
import com.olegbelyanin.expensetracker.ui.analytics.AnalyticsViewModel
import com.olegbelyanin.expensetracker.ui.categories.CategoriesViewModel
import com.olegbelyanin.expensetracker.ui.categories.CategoryFormViewModel
import com.olegbelyanin.expensetracker.ui.expense.ExpenseEditViewModel
import com.olegbelyanin.expensetracker.ui.expenses.ExpensesViewModel
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
    val categorizationConfig: CategorizationConfig = loadCategorizationConfig(context)
    val database: AppDatabase = AppDatabase.create(context.applicationContext)
    val categoryRepository: CategoryRepository = RoomCategoryRepository(database, textNormalizer, clock)
    val expenseRepository: ExpenseRepository = RoomExpenseRepository(database, textNormalizer, clock)
    val locationRepository: LocationRepository = RoomLocationRepository(database, textNormalizer)
    val learningRepository: LearningRepository = RoomLearningRepository(database)
    val observeRememberedRuleCount: ObserveRememberedRuleCountUseCase =
        ObserveRememberedRuleCountUseCase(learningRepository)
    val saveExpense: SaveExpenseUseCase = SaveExpenseUseCase(
        expenses = expenseRepository,
        categories = categoryRepository,
        validator = ExpenseInputValidator(),
        clock = clock,
        zoneId = zoneId,
    )
    val deleteExpense: DeleteExpenseUseCase = DeleteExpenseUseCase(expenseRepository)
    val clearExpenseHistory: ClearExpenseHistoryUseCase = ClearExpenseHistoryUseCase(expenseRepository)
    val suggestLocations: SuggestLocationsUseCase = SuggestLocationsUseCase(locationRepository)
    val suggestCategory: SuggestCategoryUseCase = SuggestCategoryUseCase(
        catalog = RoomCategorizationCatalog(database, textNormalizer, categorizationConfig),
        engine = CategorizationEngine(categorizationConfig),
    )
    val importExpenses: ImportExpensesUseCase = ImportExpensesUseCase(
        expenses = expenseRepository,
        suggestCategory = suggestCategory,
        clock = clock,
        zoneId = zoneId,
    )
    val recalculateCategories: RecalculateCategoriesUseCase = RecalculateCategoriesUseCase(
        expenses = expenseRepository,
        locations = locationRepository,
        suggestCategory = suggestCategory,
    )
    val categoryIconSuggester: CategoryIconSuggester = CategoryIconSuggester(textNormalizer)
    val createCategory: CreateCategoryUseCase = CreateCategoryUseCase(
        categoryRepository,
        categoryIconSuggester::suggest,
    )
    val updateCategory: UpdateCategoryUseCase = UpdateCategoryUseCase(
        categoryRepository,
        categoryIconSuggester::suggest,
    )
    val archiveCategory: ArchiveCategoryUseCase = ArchiveCategoryUseCase(categoryRepository)
    val restoreCategory: RestoreCategoryUseCase = RestoreCategoryUseCase(categoryRepository)
    val observeExpenseList: ObserveExpenseListUseCase = ObserveExpenseListUseCase(
        expenses = expenseRepository,
        categories = categoryRepository,
        locations = locationRepository,
        clock = clock,
        zoneId = zoneId,
    )
    val observeAnalytics: ObserveAnalyticsUseCase = ObserveAnalyticsUseCase(
        expenses = expenseRepository,
        categories = categoryRepository,
        clock = clock,
        zoneId = zoneId,
    )
    val themeRepository: ThemeRepository = ThemeRepository(context)

    fun expensesViewModelFactory() = ExpensesViewModel.factory(
        observeExpenseList,
        deleteExpense,
        categoryRepository,
        locationRepository,
        clock,
        zoneId,
    )

    fun analyticsViewModelFactory() = AnalyticsViewModel.factory(observeAnalytics, clock, zoneId)

    fun categoriesViewModelFactory() = CategoriesViewModel.factory(
        categories = categoryRepository,
        expenses = expenseRepository,
        archiveCategory = archiveCategory,
        restoreCategory = restoreCategory,
        clock = clock,
        zoneId = zoneId,
    )

    fun categoryFormViewModelFactory(categoryId: Long?) = CategoryFormViewModel.factory(
        categoryId = categoryId,
        categories = categoryRepository,
        createCategory = createCategory,
        updateCategory = updateCategory,
        iconSuggester = categoryIconSuggester,
        normalizer = textNormalizer,
    )

    fun expenseEditViewModelFactory(expenseId: String?) = ExpenseEditViewModel.factory(
        expenseId = expenseId,
        expenses = expenseRepository,
        categories = categoryRepository,
        locations = locationRepository,
        saveExpense = saveExpense,
        deleteExpense = deleteExpense,
        suggestLocations = suggestLocations,
        suggestCategory = suggestCategory,
        createCategory = createCategory,
        clock = clock,
        zoneId = zoneId,
    )

    init {
        runBlocking {
            SeedImporter(
                database = database,
                assets = context.applicationContext.assets,
                normalizer = textNormalizer,
            ).importIfNeeded()
        }
    }

    private fun loadCategorizationConfig(context: Context): CategorizationConfig =
        context.applicationContext.assets.open("seed/categorization-config.json").bufferedReader().use { reader ->
            CategorizationConfig.fromJson(reader.readText())
        }
}
