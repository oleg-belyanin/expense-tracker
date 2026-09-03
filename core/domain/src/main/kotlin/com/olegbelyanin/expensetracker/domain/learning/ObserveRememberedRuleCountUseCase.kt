package com.olegbelyanin.expensetracker.domain.learning

import com.olegbelyanin.expensetracker.domain.LearningRepository
import kotlinx.coroutines.flow.Flow

class ObserveRememberedRuleCountUseCase(private val learning: LearningRepository) {
    operator fun invoke(): Flow<Int> = learning.observeRememberedRuleCount()
}
