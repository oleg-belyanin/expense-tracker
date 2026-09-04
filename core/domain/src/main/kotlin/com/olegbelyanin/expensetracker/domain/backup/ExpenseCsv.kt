package com.olegbelyanin.expensetracker.domain.backup

import java.time.Instant

object ExpenseCsv {
    const val CURRENCY = "RUB"

    val HEADER: List<String> = listOf(
        "id",
        "spent_at",
        "amount",
        "amount_minor",
        "currency",
        "name",
        "category",
        "category_code",
        "location",
        "comment",
        "assignment_source",
        "dedup_key",
    )

    data class Row(
        val id: String,
        val spentAt: Instant,
        val amountMinor: Long,
        val name: String,
        val categoryName: String,
        val categoryCode: String? = null,
        val locationName: String? = null,
        val comment: String? = null,
        val assignmentSource: String,
        val dedupKey: String,
    )

    fun write(rows: List<Row>): String {
        val out = StringBuilder()
        out.appendLine(HEADER.joinToString(",") { escape(it) })
        rows.forEach { row ->
            out.appendLine(
                listOf(
                    row.id,
                    row.spentAt.toString(),
                    formatAmount(row.amountMinor),
                    row.amountMinor.toString(),
                    CURRENCY,
                    row.name,
                    row.categoryName,
                    row.categoryCode.orEmpty(),
                    row.locationName.orEmpty(),
                    row.comment.orEmpty(),
                    row.assignmentSource,
                    row.dedupKey,
                ).joinToString(",") { escape(it) },
            )
        }
        return out.toString()
    }

    fun parse(text: String): List<Row> {
        val lines = splitRecords(text)
        if (lines.isEmpty()) return emptyList()
        val header = parseRecord(lines.first())
        if (header != HEADER) {
            error("Unexpected CSV header: $header")
        }
        return lines.drop(1).filter { it.isNotBlank() }.map { line ->
            val cells = parseRecord(line)
            require(cells.size == HEADER.size) { "CSV row has ${cells.size} columns" }
            Row(
                id = cells[0],
                spentAt = Instant.parse(cells[1]),
                amountMinor = cells[3].toLong(),
                name = cells[5],
                categoryName = cells[6],
                categoryCode = cells[7].ifEmpty { null },
                locationName = cells[8].ifEmpty { null },
                comment = cells[9].ifEmpty { null },
                assignmentSource = cells[10],
                dedupKey = cells[11],
            )
        }
    }

    fun formatAmount(amountMinor: Long): String {
        val sign = if (amountMinor < 0) "-" else ""
        val abs = kotlin.math.abs(amountMinor)
        return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun splitRecords(text: String): List<String> {
        val records = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < text.length) {
            val ch = text[index]
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                    current.append(ch)
                }

                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && index + 1 < text.length && text[index + 1] == '\n') {
                        index++
                    }
                    records += current.toString()
                    current.clear()
                }

                else -> current.append(ch)
            }
            index++
        }
        if (current.isNotEmpty()) {
            records += current.toString()
        }
        return records
    }

    private fun parseRecord(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val ch = line[index]
            when {
                ch == '"' -> {
                    if (inQuotes && index + 1 < line.length && line[index + 1] == '"') {
                        current.append('"')
                        index++
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                ch == ',' && !inQuotes -> {
                    cells += current.toString()
                    current.clear()
                }

                else -> current.append(ch)
            }
            index++
        }
        cells += current.toString()
        return cells
    }
}
