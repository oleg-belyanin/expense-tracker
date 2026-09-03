package com.olegbelyanin.expensetracker.database.seed

import kotlinx.serialization.Serializable

@Serializable
data class SeedManifestDto(
    val seedDataVersion: Int,
    val normalizerVersion: Int,
    val generatedAt: String,
    val trainRows: Int,
    val validationRows: Int,
    val validationTop1Accuracy: Double? = null,
    val keywordFeatures: Int,
    val locationFeatures: Int,
)

@Serializable
data class SeedKeywordStatDto(
    val keyword: String,
    val kind: String = "word",
    val category_code: String,
    val count: Int,
)

@Serializable
data class SeedLocationStatDto(val location: String, val category_code: String, val count: Int)

@Serializable
data class SeedNameContextDto(
    val normalized_name: String,
    val category_code: String,
    val keywords: List<String> = emptyList(),
)

@Serializable
data class SeedExactRuleDto(val normalized_name: String, val category_code: String)

data class SeedSnapshot(
    val keywordStats: List<SeedKeywordStatDto> = emptyList(),
    val locationStats: List<SeedLocationStatDto> = emptyList(),
    val contexts: List<SeedNameContextDto> = emptyList(),
    val exactRules: List<SeedExactRuleDto> = emptyList(),
)
