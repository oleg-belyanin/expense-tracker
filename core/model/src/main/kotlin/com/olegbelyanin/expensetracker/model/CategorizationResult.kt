package com.olegbelyanin.expensetracker.model

data class CategorizationCandidate(val categoryId: Long, val score: Double)

data class MatchedFeature(val value: String, val kind: KeywordKind, val source: String)

data class CategorizationResult(
    val selectedCategoryId: Long,
    val orderedCandidates: List<CategorizationCandidate>,
    val source: CategoryAssignmentSource,
    val confidence: Double,
    val matchedFeatures: List<MatchedFeature>,
    val usedFallback: Boolean,
)
