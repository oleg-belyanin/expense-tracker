package com.olegbelyanin.expensetracker.model

import java.time.Instant

data class Location(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val usageCount: Int,
    val lastUsedAt: Instant?,
    val archivedAt: Instant?,
)
