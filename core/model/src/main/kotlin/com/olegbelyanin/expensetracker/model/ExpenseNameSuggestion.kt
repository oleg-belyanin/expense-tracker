package com.olegbelyanin.expensetracker.model

import java.time.Instant

data class ExpenseNameSuggestion(
    val name: String,
    val normalizedName: String,
    val usageCount: Int,
    val lastUsedAt: Instant?,
    val fromDictionary: Boolean = false,
)
