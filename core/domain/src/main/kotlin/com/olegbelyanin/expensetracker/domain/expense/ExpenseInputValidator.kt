package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.Money
import java.time.LocalDate

class ExpenseInputValidator {
    fun validate(command: SaveExpenseCommand, today: LocalDate): ExpenseValidationErrors = ExpenseValidationErrors(
        amount = amountError(command.amountInput),
        date = dateError(command.spentAt, today),
        name = nameError(command.name),
    )

    fun parseAmount(raw: String): AmountParseResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return AmountParseResult.Empty
        val cleaned = stripAmountDecorations(trimmed)
        if (cleaned.isEmpty()) return AmountParseResult.Empty
        val unsigned = when {
            cleaned.startsWith("-") || cleaned.startsWith("−") -> return AmountParseResult.Negative
            cleaned.startsWith("+") -> cleaned.drop(1)
            else -> cleaned
        }
        if (unsigned.isEmpty()) return AmountParseResult.NonNumeric
        val match = AMOUNT_REGEX.matchEntire(unsigned) ?: return AmountParseResult.NonNumeric
        val major = match.groupValues[1].toLongOrNull() ?: return AmountParseResult.NonNumeric
        val fraction = match.groupValues[2]
        val minorPart = when (fraction.length) {
            0 -> 0
            1 -> fraction.toInt() * 10
            2 -> fraction.toInt()
            else -> return AmountParseResult.NonNumeric
        }
        val total = try {
            Math.addExact(Math.multiplyExact(major, 100L), minorPart.toLong())
        } catch (_: ArithmeticException) {
            return AmountParseResult.NonNumeric
        }
        if (total == 0L) return AmountParseResult.Zero
        return AmountParseResult.Valid(Money(total))
    }

    private fun amountError(raw: String): AmountFieldError? = when (parseAmount(raw)) {
        AmountParseResult.Empty -> AmountFieldError.EMPTY
        AmountParseResult.Zero -> AmountFieldError.ZERO
        AmountParseResult.Negative -> AmountFieldError.NEGATIVE
        AmountParseResult.NonNumeric -> AmountFieldError.NON_NUMERIC
        is AmountParseResult.Valid -> null
    }

    private fun dateError(spentAt: LocalDate?, today: LocalDate): DateFieldError? =
        if (spentAt != null && spentAt.isAfter(today)) DateFieldError.FUTURE else null

    private fun nameError(name: String): NameFieldError? =
        if (name.none { it.isLetterOrDigit() }) NameFieldError.EMPTY else null

    private fun stripAmountDecorations(raw: String): String {
        var value = raw
            .replace("\u00A0", "")
            .replace("\u202F", "")
            .replace(" ", "")
            .replace("₽", "")
        value = RUB_SUFFIX_REGEX.replace(value, "")
        return value
    }

    companion object {
        private val AMOUNT_REGEX = Regex("""^(\d+)(?:[.,](\d{1,2}))?$""")
        private val RUB_SUFFIX_REGEX = Regex("""(?i)(руб\.?|р\.)$""")
    }
}
