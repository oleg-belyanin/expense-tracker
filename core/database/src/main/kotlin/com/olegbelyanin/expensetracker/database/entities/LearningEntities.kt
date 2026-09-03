package com.olegbelyanin.expensetracker.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "keyword",
    indices = [Index(value = ["kind", "value"], unique = true)],
)
data class KeywordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val value: String,
    val kind: String,
)

@Entity(
    tableName = "exact_category_rule",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["category_id"])],
)
data class ExactCategoryRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    val source: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "name_category_context",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["category_id"])],
)
data class NameCategoryContextEntity(
    @PrimaryKey
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    val source: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "name_category_context_keyword",
    primaryKeys = ["normalized_name", "keyword_id"],
    foreignKeys = [
        ForeignKey(
            entity = NameCategoryContextEntity::class,
            parentColumns = ["normalized_name"],
            childColumns = ["normalized_name"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KeywordEntity::class,
            parentColumns = ["id"],
            childColumns = ["keyword_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["keyword_id", "normalized_name"])],
)
data class NameCategoryContextKeywordEntity(
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "keyword_id")
    val keywordId: Long,
)

@Entity(
    tableName = "keyword_category_stat",
    primaryKeys = ["keyword_id", "category_id", "source"],
    foreignKeys = [
        ForeignKey(
            entity = KeywordEntity::class,
            parentColumns = ["id"],
            childColumns = ["keyword_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["keyword_id", "source"]),
        Index(value = ["category_id"]),
    ],
)
data class KeywordCategoryStatEntity(
    @ColumnInfo(name = "keyword_id")
    val keywordId: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    val source: String,
    @ColumnInfo(name = "observation_count")
    val observationCount: Int,
)

@Entity(
    tableName = "location_category_stat",
    primaryKeys = ["location_id", "category_id", "source"],
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["location_id", "source"]),
        Index(value = ["category_id"]),
    ],
)
data class LocationCategoryStatEntity(
    @ColumnInfo(name = "location_id")
    val locationId: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    val source: String,
    @ColumnInfo(name = "observation_count")
    val observationCount: Int,
)

@Entity(
    tableName = "learning_example",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expense_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["proposed_category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["expense_id"], unique = true),
        Index(value = ["category_id"]),
        Index(value = ["proposed_category_id"]),
        Index(value = ["location_id"]),
    ],
)
data class LearningExampleEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "expense_id")
    val expenseId: String?,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "proposed_category_id")
    val proposedCategoryId: Long?,
    @ColumnInfo(name = "location_id")
    val locationId: Long?,
    @ColumnInfo(name = "feedback_type")
    val feedbackType: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "learning_example_keyword",
    primaryKeys = ["learning_example_id", "keyword_id"],
    foreignKeys = [
        ForeignKey(
            entity = LearningExampleEntity::class,
            parentColumns = ["id"],
            childColumns = ["learning_example_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KeywordEntity::class,
            parentColumns = ["id"],
            childColumns = ["keyword_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["keyword_id"])],
)
data class LearningExampleKeywordEntity(
    @ColumnInfo(name = "learning_example_id")
    val learningExampleId: String,
    @ColumnInfo(name = "keyword_id")
    val keywordId: Long,
)

@Entity(
    tableName = "category_transition",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["from_category_id"]),
        Index(value = ["to_category_id"]),
    ],
)
data class CategoryTransitionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "from_category_id")
    val fromCategoryId: Long,
    @ColumnInfo(name = "to_category_id")
    val toCategoryId: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "closed_at")
    val closedAt: Long? = null,
)

@Entity(
    tableName = "category_transition_keyword",
    primaryKeys = ["transition_id", "keyword_id"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryTransitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transition_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KeywordEntity::class,
            parentColumns = ["id"],
            childColumns = ["keyword_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["keyword_id", "active"])],
)
data class CategoryTransitionKeywordEntity(
    @ColumnInfo(name = "transition_id")
    val transitionId: String,
    @ColumnInfo(name = "keyword_id")
    val keywordId: Long,
    val active: Boolean,
    @ColumnInfo(name = "deactivated_at")
    val deactivatedAt: Long? = null,
)

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey
    val key: String,
    val value: String,
)
