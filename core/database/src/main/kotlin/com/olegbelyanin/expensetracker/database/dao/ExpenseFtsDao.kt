package com.olegbelyanin.expensetracker.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.olegbelyanin.expensetracker.database.entities.ExpenseFtsEntity

@Dao
interface ExpenseFtsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExpenseFtsEntity)

    @Query("DELETE FROM expense_fts WHERE rowid = :rowId")
    suspend fun deleteByRowId(rowId: Int)

    @Query("DELETE FROM expense_fts")
    suspend fun deleteAll()
}
