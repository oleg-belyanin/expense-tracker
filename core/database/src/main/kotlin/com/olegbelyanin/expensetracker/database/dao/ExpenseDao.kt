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

    @Query("SELECT COUNT(*) FROM expense")
    fun observeCount(): Flow<Int>

    @Query("SELECT rowid FROM expense WHERE id = :id LIMIT 1")
    suspend fun rowIdById(id: String): Long?

    @Query(
        """
        SELECT * FROM expense
        WHERE normalized_name LIKE :prefix ESCAPE '\'
           OR name LIKE :rawPrefix ESCAPE '\'
        ORDER BY spent_at DESC, created_at DESC
        LIMIT :limit
        """,
    )
    suspend fun suggestNamesByPrefix(prefix: String, rawPrefix: String, limit: Int): List<ExpenseEntity>

    @Query("SELECT * FROM expense ORDER BY spent_at DESC, created_at DESC LIMIT :limit")
    suspend fun recentForNames(limit: Int): List<ExpenseEntity>

    @Query(
        """
        SELECT * FROM expense
        WHERE (:fromMs IS NULL OR spent_at >= :fromMs)
          AND (:toMsExclusive IS NULL OR spent_at < :toMsExclusive)
          AND (:hasCategories = 0 OR category_id IN (:categoryIds))
          AND (:hasLocation = 0 OR location_id = :locationId)
        ORDER BY spent_at DESC, created_at DESC
        """,
    )
    fun observeMatching(
        fromMs: Long?,
        toMsExclusive: Long?,
        hasCategories: Int,
        categoryIds: List<Long>,
        hasLocation: Int,
        locationId: Long,
    ): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT expense.* FROM expense
        WHERE (:fromMs IS NULL OR expense.spent_at >= :fromMs)
          AND (:toMsExclusive IS NULL OR expense.spent_at < :toMsExclusive)
          AND (:hasCategories = 0 OR expense.category_id IN (:categoryIds))
          AND (:hasLocation = 0 OR expense.location_id = :locationId)
          AND (
            expense.rowid IN (SELECT rowid FROM expense_fts WHERE expense_fts MATCH :ftsMatch)
            OR IFNULL(expense.location_id, -1) IN (
                SELECT location.id FROM location
                INNER JOIN location_fts ON location.id = location_fts.rowid
                WHERE location_fts MATCH :ftsMatch
            )
            OR expense.normalized_name LIKE :like ESCAPE '\'
            OR expense.name LIKE :like ESCAPE '\'
            OR IFNULL(expense.comment, '') LIKE :like ESCAPE '\'
            OR expense.category_id IN (
                SELECT id FROM category
                WHERE name LIKE :like ESCAPE '\'
                   OR normalized_name LIKE :like ESCAPE '\'
            )
            OR IFNULL(expense.location_id, -1) IN (
                SELECT id FROM location
                WHERE name LIKE :like ESCAPE '\'
                   OR normalized_name LIKE :like ESCAPE '\'
            )
          )
        ORDER BY expense.spent_at DESC, expense.created_at DESC
        """,
    )
    fun observeMatchingFts(
        fromMs: Long?,
        toMsExclusive: Long?,
        hasCategories: Int,
        categoryIds: List<Long>,
        hasLocation: Int,
        locationId: Long,
        ftsMatch: String,
        like: String,
    ): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT expense.* FROM expense
        WHERE (:fromMs IS NULL OR expense.spent_at >= :fromMs)
          AND (:toMsExclusive IS NULL OR expense.spent_at < :toMsExclusive)
          AND (:hasCategories = 0 OR expense.category_id IN (:categoryIds))
          AND (:hasLocation = 0 OR expense.location_id = :locationId)
          AND (
            expense.normalized_name LIKE :like ESCAPE '\'
            OR expense.name LIKE :like ESCAPE '\'
            OR IFNULL(expense.comment, '') LIKE :like ESCAPE '\'
            OR expense.category_id IN (
                SELECT id FROM category
                WHERE name LIKE :like ESCAPE '\'
                   OR normalized_name LIKE :like ESCAPE '\'
            )
            OR IFNULL(expense.location_id, -1) IN (
                SELECT id FROM location
                WHERE name LIKE :like ESCAPE '\'
                   OR normalized_name LIKE :like ESCAPE '\'
            )
          )
        ORDER BY expense.spent_at DESC, expense.created_at DESC
        """,
    )
    fun observeMatchingLike(
        fromMs: Long?,
        toMsExclusive: Long?,
        hasCategories: Int,
        categoryIds: List<Long>,
        hasLocation: Int,
        locationId: Long,
        like: String,
    ): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ExpenseEntity)

    @Update
    suspend fun update(entity: ExpenseEntity)

    @Query("DELETE FROM expense WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM expense")
    suspend fun deleteAll()
}
