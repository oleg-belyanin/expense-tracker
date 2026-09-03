package com.olegbelyanin.expensetracker.seed

import java.nio.file.Path
import kotlin.io.path.useLines

object DatasetReader {
    private val requiredHeader = listOf("name", "location", "category_code")

    fun read(path: Path, catalog: CategoryCatalog): List<DatasetRow> {
        path.useLines { lines ->
            val iterator = lines.iterator()
            require(iterator.hasNext()) { "Empty CSV: $path" }
            val header = parseCsvLine(iterator.next())
            require(header == requiredHeader) {
                "Unexpected CSV header $header in $path, expected $requiredHeader"
            }
            return iterator.asSequence()
                .filter { it.isNotBlank() }
                .mapIndexed { index, line -> parseRow(path, index + 2, line, catalog) }
                .toList()
        }
    }

    fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' -> {
                    if (quoted && index + 1 < line.length && line[index + 1] == '"') {
                        current.append('"')
                        index++
                    } else {
                        quoted = !quoted
                    }
                }

                char == ',' && !quoted -> {
                    fields += current.toString()
                    current.clear()
                }

                else -> current.append(char)
            }
            index++
        }
        require(!quoted) { "Unclosed quote in CSV line: $line" }
        fields += current.toString()
        return fields
    }

    private fun parseRow(path: Path, lineNumber: Int, line: String, catalog: CategoryCatalog): DatasetRow {
        val fields = parseCsvLine(line)
        require(fields.size == 3) { "$path:$lineNumber expected 3 fields, got ${fields.size}" }
        val name = fields[0].trim()
        require(name.isNotEmpty()) { "$path:$lineNumber name is empty" }
        val location = fields[1].trim().ifEmpty { null }
        val code = fields[2].trim()
        require(code in catalog.idsByCode) { "$path:$lineNumber unknown category_code=$code" }
        return DatasetRow(name, location, code)
    }
}
