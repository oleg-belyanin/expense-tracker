package com.olegbelyanin.expensetracker.model

import java.time.LocalDate

data class Period(val startInclusive: LocalDate, val endInclusive: LocalDate) {
    init {
        require(!endInclusive.isBefore(startInclusive)) {
            "Period end must not be before start"
        }
    }
}
