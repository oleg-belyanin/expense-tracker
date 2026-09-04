package com.olegbelyanin.expensetracker.database.dao

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.ExactCategoryRuleEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleEntity
import com.olegbelyanin.expensetracker.database.entities.LearningExampleKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.LocationCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextKeywordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningDao {
    @Query("SELECT * FROM learning_example WHERE expense_id = :expenseId LIMIT 1")
    suspend fun findExampleByExpenseId(expenseId: String): LearningExampleEntity?

    @Query("SELECT * FROM learning_example WHERE id = :id LIMIT 1")
    suspend fun findExampleById(id: String): LearningExampleEntity?

    @Query("UPDATE learning_example SET expense_id = NULL WHERE expense_id IS NOT NULL")
    suspend fun detachExamplesFromExpenses()

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

    @Query("SELECT * FROM name_category_context WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun findNameContext(normalizedName: String): NameCategoryContextEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNameContext(entity: NameCategoryContextEntity)

    @Query("SELECT keyword_id FROM name_category_context_keyword WHERE normalized_name = :normalizedName")
    suspend fun nameContextKeywordIds(normalizedName: String): List<Long>

    @Query("DELETE FROM name_category_context_keyword WHERE normalized_name = :normalizedName")
    suspend fun deleteNameContextKeywords(normalizedName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNameContextKeyword(entity: NameCategoryContextKeywordEntity)

    @Query("SELECT * FROM keyword_category_stat WHERE keyword_id = :keywordId")
    suspend fun statsForKeyword(keywordId: Long): List<KeywordCategoryStatEntity>

    @Query("SELECT * FROM location_category_stat WHERE location_id = :locationId")
    suspend fun statsForLocation(locationId: Long): List<LocationCategoryStatEntity>

    @Query("SELECT * FROM exact_category_rule WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun findExactRule(normalizedName: String): ExactCategoryRuleEntity?

    @Query(
        """
        SELECT * FROM exact_category_rule
        WHERE normalized_name LIKE :prefix ESCAPE '\'
        ORDER BY length(normalized_name) ASC, normalized_name ASC
        LIMIT :limit
        """,
    )
    suspend fun findExactRulesByPrefix(prefix: String, limit: Int): List<ExactCategoryRuleEntity>

    @Query(
        """
        SELECT * FROM name_category_context
        WHERE normalized_name LIKE :prefix ESCAPE '\'
        ORDER BY length(normalized_name) ASC, normalized_name ASC
        LIMIT :limit
        """,
    )
    suspend fun findNameContextsByPrefix(prefix: String, limit: Int): List<NameCategoryContextEntity>

    @Query(
        """
        SELECT COUNT(*) FROM exact_category_rule
        WHERE source IN ('explicit', 'correction')
        """,
    )
    fun observeUserExactRuleCount(): Flow<Long>

    @Query("SELECT COUNT(DISTINCT keyword_id) FROM keyword_category_stat WHERE source = 'user'")
    fun observeUserKeywordRuleCount(): Flow<Long>

    @Query("SELECT COUNT(DISTINCT location_id) FROM location_category_stat WHERE source = 'user'")
    fun observeUserLocationRuleCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM exact_category_rule WHERE source = 'seed'")
    fun observeSeedExactRuleCount(): Flow<Long>

    @Query("SELECT COUNT(DISTINCT keyword_id) FROM keyword_category_stat WHERE source = 'seed'")
    fun observeSeedKeywordRuleCount(): Flow<Long>

    @Query("SELECT COUNT(DISTINCT location_id) FROM location_category_stat WHERE source = 'seed'")
    fun observeSeedLocationRuleCount(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExactRule(entity: ExactCategoryRuleEntity)

    @Query("DELETE FROM keyword_category_stat WHERE source = :source")
    suspend fun deleteKeywordStatsBySource(source: String)

    @Query("DELETE FROM location_category_stat WHERE source = :source")
    suspend fun deleteLocationStatsBySource(source: String)

    @Query("SELECT normalized_name FROM name_category_context WHERE source = :source")
    suspend fun nameContextNamesBySource(source: String): List<String>

    @Query("DELETE FROM name_category_context WHERE normalized_name = :normalizedName AND source = :source")
    suspend fun deleteNameContextIfSource(normalizedName: String, source: String)

    @Query("SELECT normalized_name FROM exact_category_rule WHERE source = :source")
    suspend fun exactRuleNamesBySource(source: String): List<String>

    @Query("DELETE FROM exact_category_rule WHERE normalized_name = :normalizedName AND source = :source")
    suspend fun deleteExactRuleIfSource(normalizedName: String, source: String)

    @Query("SELECT keyword_id FROM category_transition_keyword WHERE active = 1")
    suspend fun activeTransitionKeywordIds(): List<Long>

    @Insert
    suspend fun insertTransition(entity: CategoryTransitionEntity)

    @Query("SELECT * FROM category_transition WHERE id = :id LIMIT 1")
    suspend fun findTransition(id: String): CategoryTransitionEntity?

    @Query("UPDATE category_transition SET closed_at = :closedAt WHERE id = :id")
    suspend fun closeTransition(id: String, closedAt: Long)

    @Query(
        """
        SELECT DISTINCT name_category_context.category_id
        FROM name_category_context_keyword
        INNER JOIN name_category_context
            ON name_category_context.normalized_name = name_category_context_keyword.normalized_name
        WHERE name_category_context_keyword.keyword_id = :keywordId
        """,
    )
    suspend fun contextCategoryIdsForKeyword(keywordId: Long): List<Long>

    @Query(
        """
        SELECT * FROM category_transition_keyword
        WHERE keyword_id = :keywordId AND active = 1
        LIMIT 1
        """,
    )
    suspend fun findActiveByKeyword(keywordId: Long): CategoryTransitionKeywordEntity?

    @Query(
        """
        SELECT COUNT(*) FROM category_transition_keyword
        WHERE transition_id = :transitionId AND active = 1
        """,
    )
    suspend fun countActiveKeywords(transitionId: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransitionKeyword(entity: CategoryTransitionKeywordEntity)

    @Query(
        """
        UPDATE category_transition_keyword
        SET active = 0, deactivated_at = :deactivatedAt
        WHERE transition_id = :transitionId AND keyword_id = :keywordId
        """,
    )
    suspend fun deactivateTransitionKeyword(transitionId: String, keywordId: Long, deactivatedAt: Long)

    @Query(
        """
        SELECT category_transition_keyword.keyword_id AS keyword_id,
               category_transition.from_category_id AS from_category_id,
               category_transition.to_category_id AS to_category_id
        FROM category_transition_keyword
        INNER JOIN category_transition
            ON category_transition.id = category_transition_keyword.transition_id
        WHERE category_transition_keyword.active = 1
          AND category_transition.closed_at IS NULL
          AND category_transition_keyword.keyword_id IN (:keywordIds)
        """,
    )
    suspend fun activeTransitionsForKeywords(keywordIds: List<Long>): List<ActiveKeywordTransitionRow>

    @Query("SELECT * FROM exact_category_rule")
    suspend fun getAllExactRules(): List<ExactCategoryRuleEntity>

    @Query("SELECT * FROM name_category_context")
    suspend fun getAllNameContexts(): List<NameCategoryContextEntity>

    @Query("SELECT * FROM name_category_context_keyword")
    suspend fun getAllNameContextKeywords(): List<NameCategoryContextKeywordEntity>

    @Query("SELECT * FROM learning_example")
    suspend fun getAllExamples(): List<LearningExampleEntity>

    @Query("SELECT * FROM learning_example_keyword")
    suspend fun getAllExampleKeywords(): List<LearningExampleKeywordEntity>

    @Query("SELECT * FROM category_transition")
    suspend fun getAllTransitions(): List<CategoryTransitionEntity>

    @Query("SELECT * FROM category_transition_keyword")
    suspend fun getAllTransitionKeywords(): List<CategoryTransitionKeywordEntity>

    @Query("SELECT * FROM keyword_category_stat")
    suspend fun getAllKeywordStats(): List<KeywordCategoryStatEntity>

    @Query("SELECT * FROM location_category_stat")
    suspend fun getAllLocationStats(): List<LocationCategoryStatEntity>
}

data class ActiveKeywordTransitionRow(
    @ColumnInfo(name = "keyword_id")
    val keywordId: Long,
    @ColumnInfo(name = "from_category_id")
    val fromCategoryId: Long,
    @ColumnInfo(name = "to_category_id")
    val toCategoryId: Long,
)
