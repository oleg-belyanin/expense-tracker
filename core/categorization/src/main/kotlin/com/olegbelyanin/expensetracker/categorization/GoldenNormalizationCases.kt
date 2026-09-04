package com.olegbelyanin.expensetracker.categorization

/**
 * Общий контракт нормализатора для приложения и `:seed-generator` (§18.1 AD-CAT-001).
 * Имена совпадают с golden-строками seed-плана §5.2.
 */
object GoldenNormalizationCases {
    data class Case(
        val rawName: String,
        val normalizedName: String,
        val rawLocation: String? = null,
        val normalizedLocation: String? = null,
    )

    val all: List<Case> = listOf(
        Case("Латте", "латт", "Шоколадница", "шоколадница"),
        Case("Кетостерил", "кетостер", "Столичка на Чкалова", "столичка на чкалова"),
        Case("Бензин", "бензин", "Лукойл", "лукойл"),
        Case("Хлеб", "хлеб"),
        Case("Непонятная покупка", "непонятн покупк"),
        Case("Врач", "врач"),
        Case("Стоматолог", "стоматолог"),
    )
}
