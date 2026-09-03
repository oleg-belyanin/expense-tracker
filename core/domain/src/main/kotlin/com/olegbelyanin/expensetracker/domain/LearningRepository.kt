package com.olegbelyanin.expensetracker.domain

import kotlinx.coroutines.flow.Flow

interface LearningRepository {
    fun observeRememberedRuleCount(): Flow<Int>
}
