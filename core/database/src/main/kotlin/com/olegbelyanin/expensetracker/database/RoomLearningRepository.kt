package com.olegbelyanin.expensetracker.database

import com.olegbelyanin.expensetracker.domain.LearningRepository
import com.olegbelyanin.expensetracker.domain.learning.RememberedRuleCounts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoomLearningRepository(private val database: AppDatabase) : LearningRepository {
    override fun observeRememberedRuleCounts(): Flow<RememberedRuleCounts> {
        val learning = database.learningDao()
        return combine(
            learning.observeUserExactRuleCount(),
            learning.observeUserKeywordRuleCount(),
            learning.observeUserLocationRuleCount(),
        ) { exact, keywords, locations ->
            RememberedRuleCounts(
                exactRules = exact.toInt(),
                keywordRules = keywords.toInt(),
                locationRules = locations.toInt(),
            )
        }
    }
}
