package com.olegbelyanin.expensetracker.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts5
import androidx.room3.PrimaryKey

@Fts5(tokenizer = "unicode61")
@Entity(tableName = "expense_fts")
data class ExpenseFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int,
    val name: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    val comment: String,
)

@Fts5(tokenizer = "unicode61")
@Entity(tableName = "location_fts")
data class LocationFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int,
    val name: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
)
