package com.olegbelyanin.expensetracker.model

import java.time.Instant

data class Category(
    val id: Long,
    val code: String?,
    val name: String,
    val normalizedName: String,
    val color: String,
    val icon: String,
    val isBuiltin: Boolean,
    val archivedAt: Instant?,
) {
    val isFallback: Boolean
        get() = isBuiltin && code == BuiltinCategories.FALLBACK_CODE

    val isActive: Boolean
        get() = archivedAt == null
}
