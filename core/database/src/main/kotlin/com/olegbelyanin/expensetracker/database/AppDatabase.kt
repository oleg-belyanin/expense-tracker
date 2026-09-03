package com.olegbelyanin.expensetracker.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.olegbelyanin.expensetracker.database.dao.CategoryDao
import com.olegbelyanin.expensetracker.database.dao.KeywordDao
import com.olegbelyanin.expensetracker.database.dao.LocationDao
import com.olegbelyanin.expensetracker.database.dao.MetaDao
import com.olegbelyanin.expensetracker.database.dao.SeedDao
import com.olegbelyanin.expensetracker.database.entities.AppMetaEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.ExactCategoryRuleEntity
import com.olegbelyanin.expensetracker.database.entities.ExpenseEntity
import com.olegbelyanin.expensetracker.database.entities.ExpenseFtsEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LocationCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.LocationEntity
import com.olegbelyanin.expensetracker.database.entities.LocationFtsEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextKeywordEntity
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        CategoryEntity::class,
        LocationEntity::class,
        ExpenseEntity::class,
        ExpenseFtsEntity::class,
        LocationFtsEntity::class,
        KeywordEntity::class,
        ExactCategoryRuleEntity::class,
        NameCategoryContextEntity::class,
        NameCategoryContextKeywordEntity::class,
        KeywordCategoryStatEntity::class,
        LocationCategoryStatEntity::class,
        LearningExampleEntity::class,
        LearningExampleKeywordEntity::class,
        CategoryTransitionEntity::class,
        CategoryTransitionKeywordEntity::class,
        AppMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    abstract fun locationDao(): LocationDao

    abstract fun keywordDao(): KeywordDao

    abstract fun seedDao(): SeedDao

    abstract fun metaDao(): MetaDao

    companion object {
        const val NAME = "expense-tracker.db"

        fun create(context: Context): AppDatabase = Room.databaseBuilder<AppDatabase>(context, NAME)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
