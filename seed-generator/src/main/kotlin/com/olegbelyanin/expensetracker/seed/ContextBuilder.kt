package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.model.KeywordKind

object ContextBuilder {
    fun build(rows: List<DatasetRow>, normalizer: TextNormalizer): List<SeedNameContextDto> {
        val last = linkedMapOf<String, SeedNameContextDto>()
        rows.forEach { row ->
            val analysis = normalizer.analyze(row.name)
            if (analysis.normalizedName.isEmpty()) return@forEach
            last[analysis.normalizedName] = SeedNameContextDto(
                normalized_name = analysis.normalizedName,
                category_code = row.categoryCode,
                keywords = analysis.features
                    .filter { it.kind == KeywordKind.WORD }
                    .map { it.value }
                    .distinct(),
            )
        }
        return last.values.toList()
    }
}
