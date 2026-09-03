package com.olegbelyanin.expensetracker

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.data.theme.ThemeRepository
import com.olegbelyanin.expensetracker.database.AppDatabase
import com.olegbelyanin.expensetracker.database.RoomCategoryRepository
import com.olegbelyanin.expensetracker.database.seed.SeedImporter
import com.olegbelyanin.expensetracker.domain.CategoryRepository
import kotlinx.coroutines.runBlocking

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not provided")
}

class AppContainer(context: Context) {
    val textNormalizer: TextNormalizer = TextNormalizer()
    val database: AppDatabase = AppDatabase.create(context.applicationContext)
    val categoryRepository: CategoryRepository = RoomCategoryRepository(database)
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
