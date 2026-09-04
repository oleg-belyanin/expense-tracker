package com.olegbelyanin.expensetracker.domain.learning

/** Счётчики правил по источникам F-03, которые видит блок обучения в настройках. */
data class RememberedRuleCounts(
    val exactRules: Int = 0,
    val keywordRules: Int = 0,
    val locationRules: Int = 0,
)

/**
 * Что входит в видимые пользователю правила.
 * Exact — только явный выбор и исправление (F-04).
 * Слова и места — пользовательская статистика; seed-словарь и `category_name` не входят.
 */
object RememberedRules {
    const val EXPLICIT = "explicit"
    const val CORRECTION = "correction"
    const val USER_STAT = "user"

    fun countsExact(source: String): Boolean = source == EXPLICIT || source == CORRECTION

    fun countsUserStat(source: String): Boolean = source == USER_STAT

    fun counts(source: String): Boolean = countsExact(source)

    fun count(sources: Iterable<String>): Int = sources.count(::countsExact)

    fun countUserStats(sources: Iterable<String>): Int = sources.distinct().count(::countsUserStat)
}
