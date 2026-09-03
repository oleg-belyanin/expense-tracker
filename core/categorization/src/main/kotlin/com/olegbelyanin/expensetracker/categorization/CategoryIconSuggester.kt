package com.olegbelyanin.expensetracker.categorization

import com.olegbelyanin.expensetracker.model.BuiltinCategories
import com.olegbelyanin.expensetracker.model.CategoryIcons

/**
 * Автоподбор `category.icon` по нормализованному имени.
 * Пример из макета: «Питомцы» → `pets`; неизвестное имя → `letter`.
 */
class CategoryIconSuggester(private val normalizer: TextNormalizer = TextNormalizer()) {
    fun suggest(rawName: String): String {
        val analysis = normalizer.analyze(rawName)
        val tokens = buildSet {
            add(normalizer.normalizePlain(rawName))
            add(analysis.normalizedName)
            analysis.features.forEach { add(it.value) }
            analysis.normalizedName.split(' ').forEach { token ->
                if (token.isNotEmpty()) add(token)
            }
        }.filter { it.isNotEmpty() }
        for (token in tokens) {
            EXACT[token]?.let { return it }
        }
        for (token in tokens) {
            if (token.length < MIN_PREFIX_LENGTH) continue
            for ((alias, icon) in PREFIX) {
                if (token.startsWith(alias) || alias.startsWith(token)) {
                    return icon
                }
            }
        }
        return CategoryIcons.LETTER
    }

    companion object {
        private const val MIN_PREFIX_LENGTH = 4

        private val EXACT: Map<String, String> = buildMap {
            BuiltinCategories.all.forEach { spec ->
                put(spec.name.lowercase(), CategoryIcons.canonicalize(spec.icon))
            }
            CategoryIcons.catalog.filter { it != CategoryIcons.LETTER }.forEach { key ->
                put(key, key)
            }
            putAll(
                mapOf(
                    "жилье" to "housing",
                    "еда" to "groceries",
                    "супермаркет" to "groceries",
                    "кофе" to "cafe",
                    "кофейня" to "cafe",
                    "ресторан" to "cafe",
                    "такси" to "transport",
                    "метро" to "transport",
                    "бензин" to "transport",
                    "автобус" to "transport",
                    "аптека" to "health",
                    "врач" to "health",
                    "клиника" to "health",
                    "стоматолог" to "health",
                    "стоматология" to "health",
                    "квартира" to "housing",
                    "аренда" to "housing",
                    "жкх" to "housing",
                    "интернет" to "comms",
                    "телефон" to "comms",
                    "кино" to "fun",
                    "игры" to "fun",
                    "концерт" to "fun",
                    "обувь" to "clothes",
                    "хозяйство" to "home",
                    "ремонт" to "home",
                    "питомцы" to "pets",
                    "животные" to "pets",
                    "собака" to "pets",
                    "кошка" to "pets",
                    "учеба" to "education",
                    "курсы" to "education",
                    "школа" to "education",
                    "университет" to "education",
                    "шопинг" to "shopping",
                    "покупки" to "shopping",
                    "фитнес" to "sports",
                    "зал" to "sports",
                    "отпуск" to "travel",
                    "отель" to "travel",
                    "офис" to "work",
                ),
            )
        }

        private val PREFIX: List<Pair<String, String>> = listOf(
            "стоматолог" to "health",
            "ортодонт" to "health",
            "аптек" to "health",
            "транспорт" to "transport",
            "развлечен" to "fun",
            "образован" to "education",
            "путешеств" to "travel",
            "фитнес" to "sports",
        )
    }
}
