package com.olegbelyanin.expensetracker.model

/**
 * Сумма в минимальных единицах валюты MVP (копейки ₽).
 */
data class Money(val minor: Long) {
    init {
        require(minor > 0) { "Money.minor must be positive" }
    }
}
