package com.olegbelyanin.expensetracker.database

import com.olegbelyanin.expensetracker.domain.LearningRepository
import com.olegbelyanin.expensetracker.domain.learning.RememberedRuleCounts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoomLearningRepository(private val database: AppDatabase) : LearningRepository {
    override fun observeRememberedRuleCounts(): Flow<RememberedRuleCounts> {
        val learning = database.learningDao()
        return combine(
            listOf(
                learning.observeUserExactRuleCount(),
                learning.observeUserKeywordRuleCount(),
                learning.observeUserLocationRuleCount(),
                learning.observeSeedExactRuleCount(),
                learning.observeSeedKeywordRuleCount(),
                learning.observeSeedLocationRuleCount(),
            ),
        ) { values ->
            RememberedRuleCounts(
                exactRules = values[0].toInt(),
                keywordRules = values[1].toInt(),
                locationRules = values[2].toInt(),
                seedExactRules = values[3].toInt(),
                seedKeywordRules = values[4].toInt(),
                seedLocationRules = values[5].toInt(),
            )
        }
    }
}
