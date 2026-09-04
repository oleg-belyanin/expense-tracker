package com.olegbelyanin.expensetracker.database.search

import com.olegbelyanin.expensetracker.categorization.PrefixFeatureResolver
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.ExpenseNameSuggestion
import java.time.Instant
import java.util.Locale

object NameSuggestionRanker {
    fun fromExpenses(expenses: List<Expense>): List<ExpenseNameSuggestion> {
        val grouped = LinkedHashMap<String, ExpenseNameSuggestion>()
        expenses.sortedByDescending { it.spentAt }.forEach { expense ->
            val current = grouped[expense.normalizedName]
            grouped[expense.normalizedName] = if (current == null) {
                ExpenseNameSuggestion(
                    name = expense.name,
                    normalizedName = expense.normalizedName,
                    usageCount = 1,
                    lastUsedAt = expense.spentAt,
                    fromDictionary = false,
                )
            } else {
                current.copy(usageCount = current.usageCount + 1)
            }
        }
        return grouped.values.toList()
    }

    fun merge(
        queryNormalized: String,
        history: List<ExpenseNameSuggestion>,
        dictionary: List<ExpenseNameSuggestion>,
        limit: Int,
    ): List<ExpenseNameSuggestion> {
        val byKey = LinkedHashMap<String, ExpenseNameSuggestion>()
        history.forEach { byKey.putIfAbsent(it.normalizedName, it) }
        dictionary.forEach { candidate ->
            byKey.putIfAbsent(itKey(candidate), candidate)
        }
        return byKey.values
            .filter { visible(queryNormalized, it) }
            .sortedWith(
                compareBy<ExpenseNameSuggestion> { matchQuality(queryNormalized, it) }
                    .thenByDescending { it.lastUsedAt ?: Instant.EPOCH }
                    .thenByDescending { it.usageCount }
                    .thenBy { it.name.lowercase(Locale.ROOT) },
            )
            .take(limit)
    }

    fun titleCase(normalized: String): String =
        normalized.split(' ', '"')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
                }
            }

    private fun itKey(suggestion: ExpenseNameSuggestion): String = suggestion.normalizedName

    private fun visible(queryNormalized: String, suggestion: ExpenseNameSuggestion): Boolean {
        if (queryNormalized.isEmpty()) return true
        val normalized = suggestion.normalizedName
        val namePlain = suggestion.name.trim().lowercase(Locale.ROOT)
        return normalized != queryNormalized && namePlain != queryNormalized
    }

    private fun matchQuality(queryNormalized: String, suggestion: ExpenseNameSuggestion): Int {
        if (queryNormalized.isEmpty()) return 1
        val normalized = suggestion.normalizedName
        val namePlain = suggestion.name.lowercase(Locale.ROOT)
        return when {
            normalized == queryNormalized || namePlain == queryNormalized -> 0
            PrefixFeatureResolver.matches(queryNormalized, normalized) -> 1
            namePlain.startsWith(queryNormalized) -> 1
            else -> 2
        }
    }
}
