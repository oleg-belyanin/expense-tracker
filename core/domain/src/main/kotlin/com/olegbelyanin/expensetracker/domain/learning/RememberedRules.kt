package com.olegbelyanin.expensetracker.domain.learning

/** Пользовательские exact rules, которые видит строка «Запомненные правила» (F-04). */
object RememberedRules {
    const val EXPLICIT = "explicit"
    const val CORRECTION = "correction"

    fun counts(source: String): Boolean = source == EXPLICIT || source == CORRECTION

    fun count(sources: Iterable<String>): Int = sources.count(::counts)
}
