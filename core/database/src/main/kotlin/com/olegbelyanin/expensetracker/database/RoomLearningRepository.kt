package com.olegbelyanin.expensetracker.database

import com.olegbelyanin.expensetracker.domain.LearningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomLearningRepository(private val database: AppDatabase) : LearningRepository {
    override fun observeRememberedRuleCount(): Flow<Int> =
        database.learningDao().observeUserExactRuleCount().map { it.toInt() }
}
