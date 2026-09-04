package com.olegbelyanin.expensetracker.database.demo

import android.content.res.AssetManager
import com.olegbelyanin.expensetracker.database.AppDatabase
import com.olegbelyanin.expensetracker.database.entities.AppMetaEntity
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.backup.ExpenseCsv
import com.olegbelyanin.expensetracker.domain.demo.DemoExpenseCsv
import com.olegbelyanin.expensetracker.domain.expense.ImportExpensesUseCase
import java.io.IOException

/**
 * Debug-only CSV из `assets/demo/`. Release-сборка файла не содержит.
 * Повторно не импортирует после очистки истории.
 */
class DemoExpensesImporter(
    private val database: AppDatabase,
    private val assets: AssetManager,
    private val importExpenses: ImportExpensesUseCase,
    private val expenses: ExpenseRepository,
) {
    suspend fun importIfNeeded() {
        if (database.metaDao().get(META_KEY) == IMPORTED) return
        val csv = readAssetOrNull(ASSET) ?: return
        if (expenses.getAll().isEmpty()) {
            importExpenses(DemoExpenseCsv.toDrafts(ExpenseCsv.parse(csv)))
        }
        database.metaDao().put(AppMetaEntity(META_KEY, IMPORTED))
    }

    private fun readAssetOrNull(path: String): String? = try {
        assets.open(path).bufferedReader().use { it.readText() }
    } catch (_: IOException) {
        null
    }

    companion object {
        const val ASSET = "demo/expenses.csv"
        const val META_KEY = "demo_expenses_imported"
        private const val IMPORTED = "1"
    }
}
