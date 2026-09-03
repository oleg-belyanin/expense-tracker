package com.olegbelyanin.expensetracker.categorization

/**
 * Производный статистический опыт имени категории (§13.1 AD-CAT-001).
 * Не создаёт exact rule и не пишется в `learning_example`.
 */
object CategoryNameExperience {
    const val SOURCE = "category_name"

    fun features(normalizer: TextNormalizer, rawName: String): List<KeywordFeature> =
        normalizer.analyze(rawName).features.distinctBy { it.kind to it.value }
}
