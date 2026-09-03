package com.olegbelyanin.expensetracker.model

import java.time.LocalDate

data class Period(val startInclusive: LocalDate, val endInclusive: LocalDate) {
    init {
        require(!endInclusive.isBefore(startInclusive)) {
            "Period end must not be before start"
        }
    }

    companion object {
        fun of(start: LocalDate, end: LocalDate): Period =
            if (end.isBefore(start)) Period(end, start) else Period(start, end)
    }
}
