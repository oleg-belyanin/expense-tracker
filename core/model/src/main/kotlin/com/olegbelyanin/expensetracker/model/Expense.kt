package com.olegbelyanin.expensetracker.model

import java.time.Instant

data class Expense(
    val id: String,
    val amount: Money,
    val spentAt: Instant,
    val name: String,
    val normalizedName: String,
    val categoryId: Long,
    val locationId: Long?,
    val comment: String?,
    val categoryAssignmentSource: CategoryAssignmentSource,
    val dedupKey: String,
)
