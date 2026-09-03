package com.olegbelyanin.expensetracker.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "expense",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["spent_at"]),
        Index(value = ["category_id", "spent_at"]),
        Index(value = ["location_id", "spent_at"]),
        Index(value = ["normalized_name"]),
        Index(value = ["dedup_key"], unique = true),
    ],
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
    @ColumnInfo(name = "spent_at")
    val spentAt: Long,
    val name: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "location_id")
    val locationId: Long?,
    val comment: String?,
    @ColumnInfo(name = "category_assignment_source")
    val categoryAssignmentSource: String,
    @ColumnInfo(name = "dedup_key")
    val dedupKey: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
