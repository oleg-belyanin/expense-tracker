package com.olegbelyanin.expensetracker.model

/**
 * Runtime-каталог встроенных категорий.
 * Коды и имена совпадают с [seed-data/categories.yaml].
 */
data class BuiltinCategorySpec(
    val code: String,
    val name: String,
    val icon: String,
    val color: String,
    val isFallback: Boolean = false,
)

object BuiltinCategories {
    const val FALLBACK_CODE = "OTHER"

    val all: List<BuiltinCategorySpec> = listOf(
        BuiltinCategorySpec("GROCERIES", "Продукты", "groceries", "#2E7D32"),
        BuiltinCategorySpec("CAFE", "Кафе", "cafe", "#E65100"),
        BuiltinCategorySpec("TRANSPORT", "Транспорт", "transport", "#1565C0"),
        BuiltinCategorySpec("HEALTH", "Здоровье", "health", "#C62828"),
        BuiltinCategorySpec("HOUSING", "Жильё", "housing", "#6A1B9A"),
        BuiltinCategorySpec("COMMUNICATION", "Связь", "communication", "#00838F"),
        BuiltinCategorySpec("ENTERTAINMENT", "Развлечения", "entertainment", "#AD1457"),
        BuiltinCategorySpec("CLOTHING", "Одежда", "clothing", "#4527A0"),
        BuiltinCategorySpec("HOME", "Дом", "home", "#EF6C00"),
        BuiltinCategorySpec("OTHER", "Прочее", "other", "#546E7A", isFallback = true),
    )

    val fallback: BuiltinCategorySpec
        get() = all.first { it.isFallback }

    fun byCode(code: String): BuiltinCategorySpec = all.first { it.code == code }
}
