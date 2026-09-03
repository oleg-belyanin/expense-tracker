package com.olegbelyanin.expensetracker.database

import com.olegbelyanin.expensetracker.database.entities.CategoryEntity
import com.olegbelyanin.expensetracker.database.entities.ExpenseEntity
import com.olegbelyanin.expensetracker.database.entities.LocationEntity
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.Location
import com.olegbelyanin.expensetracker.model.Money
import java.time.Instant

internal fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    code = code,
    name = name,
    normalizedName = normalizedName,
    color = color,
    icon = icon,
    isBuiltin = isBuiltin,
    archivedAt = archivedAt?.let(Instant::ofEpochMilli),
)

internal fun LocationEntity.toDomain(): Location = Location(
    id = id,
    name = name,
    normalizedName = normalizedName,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt?.let(Instant::ofEpochMilli),
    archivedAt = archivedAt?.let(Instant::ofEpochMilli),
)

internal fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    amount = Money(amountMinor),
    spentAt = Instant.ofEpochMilli(spentAt),
    name = name,
    normalizedName = normalizedName,
    categoryId = categoryId,
    locationId = locationId,
    comment = comment,
    categoryAssignmentSource = CategoryAssignmentSource.valueOf(categoryAssignmentSource.uppercase()),
    dedupKey = dedupKey,
)

internal fun CategoryAssignmentSource.storageValue(): String = name.lowercase()
