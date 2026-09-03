package com.olegbelyanin.expensetracker.model

/**
 * Каталог глифов и палитра пользовательских категорий.
 * Ключи совпадают с [CategoryGlyphKey] во `:app` и с формой Figma `27:110`.
 */
object CategoryIcons {
    const val LETTER = "letter"

    val catalog: List<String> = listOf(
        "groceries",
        "cafe",
        "transport",
        "health",
        "housing",
        "comms",
        "fun",
        "clothes",
        "home",
        "other",
        "pets",
        "education",
        "shopping",
        "sports",
        "travel",
        "work",
        LETTER,
    )

    fun canonicalize(raw: String): String = when (raw) {
        "communication" -> "comms"
        "entertainment" -> "fun"
        "clothing" -> "clothes"
        else -> if (raw in catalog) raw else LETTER
    }
}

object CategoryPalette {
    const val DEFAULT = "#858A82"

    val swatches: List<String> = listOf(
        "#E6B84A",
        "#E8894A",
        "#4A9D8F",
        "#D76073",
        "#6E7FC5",
        "#4F9ACF",
        "#A76CC1",
    )

    fun swatchForIcon(icon: String): String = when (CategoryIcons.canonicalize(icon)) {
        "groceries", "shopping" -> swatches[0]
        "cafe" -> swatches[1]
        "transport", "sports", "home" -> swatches[2]
        "health", "clothes" -> swatches[3]
        "housing", "education" -> swatches[4]
        "comms", "travel" -> swatches[5]
        "fun", "pets" -> swatches[6]
        else -> swatches.first()
    }
}
