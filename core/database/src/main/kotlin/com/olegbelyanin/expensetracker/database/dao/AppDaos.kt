package com.olegbelyanin.expensetracker.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.olegbelyanin.expensetracker.database.entities.AppMetaEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryEntity
import com.olegbelyanin.expensetracker.database.entities.ExactCategoryRuleEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LocationCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.LocationEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextKeywordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category WHERE archived_at IS NULL ORDER BY id ASC")
    suspend fun getActive(): List<CategoryEntity>

    @Query("SELECT * FROM category WHERE archived_at IS NULL ORDER BY id ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE archived_at IS NOT NULL ORDER BY archived_at DESC, id ASC")
    fun observeArchived(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE is_builtin = 1 AND code = :code LIMIT 1")
    suspend fun findBuiltinByCode(code: String): CategoryEntity?

    @Query("SELECT * FROM category WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): CategoryEntity?

    @Query("SELECT * FROM category WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Long

    @Insert
    suspend fun insert(entity: CategoryEntity): Long

    @Update
    suspend fun update(entity: CategoryEntity)
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM location ORDER BY id ASC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query(
        """
        SELECT * FROM location
        WHERE archived_at IS NULL AND usage_count > 0
        ORDER BY last_used_at DESC, usage_count DESC, id ASC
        """,
    )
    fun observeUsed(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM location WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): LocationEntity?

    @Query("SELECT * FROM location WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): LocationEntity?

    @Insert
    suspend fun insert(entity: LocationEntity): Long

    @Update
    suspend fun update(entity: LocationEntity)

    @Query(
        """
        SELECT * FROM location
        WHERE archived_at IS NULL AND usage_count > 0
        ORDER BY last_used_at DESC, usage_count DESC, id ASC
        LIMIT :limit
        """,
    )
    suspend fun recentUsed(limit: Int): List<LocationEntity>

    @Query(
        """
        SELECT * FROM location
        WHERE archived_at IS NULL
          AND usage_count > 0
          AND (
            normalized_name LIKE :prefix ESCAPE '\'
            OR name LIKE :rawPrefix ESCAPE '\'
          )
        ORDER BY last_used_at DESC, usage_count DESC, id ASC
        LIMIT :limit
        """,
    )
    suspend fun suggestByPrefix(prefix: String, rawPrefix: String, limit: Int): List<LocationEntity>
}

@Dao
interface KeywordDao {
    @Query("SELECT * FROM keyword WHERE kind = :kind AND value = :value LIMIT 1")
    suspend fun find(kind: String, value: String): KeywordEntity?

    @Insert
    suspend fun insert(entity: KeywordEntity): Long
}

@Dao
interface SeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKeywordStat(entity: KeywordCategoryStatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocationStat(entity: LocationCategoryStatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNameContext(entity: NameCategoryContextEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNameContextKeyword(entity: NameCategoryContextKeywordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExactRule(entity: ExactCategoryRuleEntity)
}

@Dao
interface MetaDao {
    @Query("SELECT value FROM app_meta WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: AppMetaEntity)
}
