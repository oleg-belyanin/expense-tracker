package com.olegbelyanin.expensetracker.domain.demo

import com.olegbelyanin.expensetracker.domain.backup.ExpenseCsv
import com.olegbelyanin.expensetracker.domain.expense.ImportExpenseDraft
import com.olegbelyanin.expensetracker.model.Money

object DemoExpenseCsv {
    fun toDrafts(rows: List<ExpenseCsv.Row>): List<ImportExpenseDraft> = rows.map { row ->
        ImportExpenseDraft(
            id = row.id,
            amount = Money(row.amountMinor),
            spentAt = row.spentAt,
            name = row.name,
            locationName = row.locationName,
            comment = row.comment,
            dedupKey = row.dedupKey,
        )
    }
}
