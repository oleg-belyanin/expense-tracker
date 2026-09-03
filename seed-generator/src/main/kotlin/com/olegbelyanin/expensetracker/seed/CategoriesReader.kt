package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.model.BuiltinCategories
import java.nio.file.Path
import kotlin.io.path.readText

object CategoriesReader {
    fun read(path: Path): CategoryCatalog {
        val yaml = path.readText()
        val block = yaml.substringBefore("dataset:")
        val codes = Regex("""(?m)^\s*-?\s*code:\s*(\w+)\s*$""")
            .findAll(block)
            .map { it.groupValues[1] }
            .toList()
        require(codes.isNotEmpty()) { "No categories in $path" }
        val builtin = BuiltinCategories.all.map { it.code }
        require(codes == builtin) {
            "categories.yaml codes $codes do not match BuiltinCategories $builtin"
        }
        return CategoryCatalog.fromCodes(codes, BuiltinCategories.FALLBACK_CODE)
    }
}
