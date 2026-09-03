package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.categorization.CategorizationCatalog
import com.olegbelyanin.expensetracker.categorization.CategorizationEngine
import com.olegbelyanin.expensetracker.model.CategorizationResult

class SuggestCategoryUseCase(
    private val catalog: CategorizationCatalog,
    private val engine: CategorizationEngine = CategorizationEngine(),
) {
    suspend operator fun invoke(name: String, locationName: String?): CategorizationResult {
        val lookup = catalog.lookup(name, locationName)
        return engine.categorize(lookup.query, lookup.snapshot)
    }
}
