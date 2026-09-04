package com.olegbelyanin.expensetracker.domain.learning

/** Счётчики правил по источникам F-03, которые видит блок обучения в настройках. */
data class RememberedRuleCounts(
    val exactRules: Int = 0,
    val keywordRules: Int = 0,
    val locationRules: Int = 0,
    val seedExactRules: Int = 0,
    val seedKeywordRules: Int = 0,
    val seedLocationRules: Int = 0,
)

/**
 * Что входит в видимые правила.
 * Пользовательские exact — явный выбор и исправление (F-04).
 * Слова и места — уникальные признаки в статистике, не пары признак×категория.
 * Seed считается отдельно; `category_name` не входит ни в один счётчик.
 */
object RememberedRules {
    const val EXPLICIT = "explicit"
    const val CORRECTION = "correction"
    const val USER_STAT = "user"
    const val SEED = "seed"

    fun countsExact(source: String): Boolean = source == EXPLICIT || source == CORRECTION

    fun countsSeedExact(source: String): Boolean = source == SEED

    fun countsUserStat(source: String): Boolean = source == USER_STAT

    fun countsSeedStat(source: String): Boolean = source == SEED

    fun counts(source: String): Boolean = countsExact(source)

    fun count(sources: Iterable<String>): Int = sources.count(::countsExact)

    fun countUserStats(sources: Iterable<String>): Int = sources.distinct().count(::countsUserStat)

    fun countSeedStats(sources: Iterable<String>): Int = sources.distinct().count(::countsSeedStat)
}
