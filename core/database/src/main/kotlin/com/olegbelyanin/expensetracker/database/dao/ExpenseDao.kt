package com.olegbelyanin.expensetracker.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.olegbelyanin.expensetracker.database.entities.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expense WHERE dedup_key = :dedupKey LIMIT 1")
    suspend fun findByDedupKey(dedupKey: String): ExpenseEntity?

    @Query("SELECT * FROM expense ORDER BY spent_at DESC, created_at DESC")
    suspend fun getAll(): List<ExpenseEntity>

    @Query("SELECT * FROM expense ORDER BY spent_at DESC, created_at DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ExpenseEntity)

    @Update
    suspend fun update(entity: ExpenseEntity)

    @Query("DELETE FROM expense WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM expense")
    suspend fun deleteAll()
}
