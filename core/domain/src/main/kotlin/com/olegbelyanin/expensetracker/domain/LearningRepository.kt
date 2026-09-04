package com.olegbelyanin.expensetracker.domain

import com.olegbelyanin.expensetracker.domain.learning.RememberedRuleCounts
import kotlinx.coroutines.flow.Flow

interface LearningRepository {
    fun observeRememberedRuleCounts(): Flow<RememberedRuleCounts>
}
