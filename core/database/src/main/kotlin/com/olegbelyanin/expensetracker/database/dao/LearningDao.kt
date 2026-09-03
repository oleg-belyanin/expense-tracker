package com.olegbelyanin.expensetracker.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionEntity
import com.olegbelyanin.expensetracker.database.entities.ExactCategoryRuleEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LocationCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextKeywordEntity

@Dao
interface LearningDao {
    @Query("SELECT * FROM learning_example WHERE expense_id = :expenseId LIMIT 1")
    suspend fun findExampleByExpenseId(expenseId: String): LearningExampleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExample(entity: LearningExampleEntity)

    @Query("SELECT keyword_id FROM learning_example_keyword WHERE learning_example_id = :exampleId")
    suspend fun exampleKeywordIds(exampleId: String): List<Long>

    @Query("DELETE FROM learning_example_keyword WHERE learning_example_id = :exampleId")
    suspend fun deleteExampleKeywords(exampleId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExampleKeyword(entity: LearningExampleKeywordEntity)

    @Query(
        """
        SELECT * FROM keyword_category_stat
        WHERE keyword_id = :keywordId AND category_id = :categoryId AND source = :source
        LIMIT 1
        """,
    )
    suspend fun findKeywordStat(keywordId: Long, categoryId: Long, source: String): KeywordCategoryStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKeywordStat(entity: KeywordCategoryStatEntity)

    @Query(
        """
        DELETE FROM keyword_category_stat
        WHERE keyword_id = :keywordId AND category_id = :categoryId AND source = :source
        """,
    )
    suspend fun deleteKeywordStat(keywordId: Long, categoryId: Long, source: String)

    @Query("SELECT COUNT(*) FROM keyword_category_stat WHERE category_id = :categoryId AND source = :source")
    suspend fun countKeywordStats(categoryId: Long, source: String): Long

    @Query("DELETE FROM keyword_category_stat WHERE category_id = :categoryId AND source = :source")
    suspend fun deleteKeywordStats(categoryId: Long, source: String)

    @Query(
        """
        SELECT * FROM location_category_stat
        WHERE location_id = :locationId AND category_id = :categoryId AND source = :source
        LIMIT 1
        """,
    )
    suspend fun findLocationStat(locationId: Long, categoryId: Long, source: String): LocationCategoryStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocationStat(entity: LocationCategoryStatEntity)

    @Query(
        """
        DELETE FROM location_category_stat
        WHERE location_id = :locationId AND category_id = :categoryId AND source = :source
        """,
    )
    suspend fun deleteLocationStat(locationId: Long, categoryId: Long, source: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNameContext(entity: NameCategoryContextEntity)

    @Query("DELETE FROM name_category_context_keyword WHERE normalized_name = :normalizedName")
    suspend fun deleteNameContextKeywords(normalizedName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNameContextKeyword(entity: NameCategoryContextKeywordEntity)

    @Query("SELECT * FROM exact_category_rule WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun findExactRule(normalizedName: String): ExactCategoryRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExactRule(entity: ExactCategoryRuleEntity)

    @Insert
    suspend fun insertTransition(entity: CategoryTransitionEntity)
}
