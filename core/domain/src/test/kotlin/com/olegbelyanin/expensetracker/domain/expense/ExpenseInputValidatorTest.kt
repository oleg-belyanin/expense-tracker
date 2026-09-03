package com.olegbelyanin.expensetracker.domain.expense

import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpenseInputValidatorTest {
    private val validator = ExpenseInputValidator()
    private val today = LocalDate.of(2026, 9, 3)

    @Test
    fun amountEmptyZeroNegativeAndNonNumeric() {
        assertEquals(AmountParseResult.Empty, validator.parseAmount(""))
        assertEquals(AmountParseResult.Empty, validator.parseAmount("   "))
        assertEquals(AmountParseResult.Zero, validator.parseAmount("0"))
        assertEquals(AmountParseResult.Zero, validator.parseAmount("0,00"))
        assertEquals(AmountParseResult.Zero, validator.parseAmount("0.0"))
        assertEquals(AmountParseResult.Negative, validator.parseAmount("-10"))
        assertEquals(AmountParseResult.Negative, validator.parseAmount("−1"))
        assertEquals(AmountParseResult.NonNumeric, validator.parseAmount("abc"))
        assertEquals(AmountParseResult.NonNumeric, validator.parseAmount("12a"))
        assertEquals(AmountParseResult.NonNumeric, validator.parseAmount("1.2.3"))
        assertEquals(AmountParseResult.NonNumeric, validator.parseAmount("150,505"))
    }

    @Test
    fun amountParsesRublesAndKopecks() {
        assertEquals(AmountParseResult.Valid(Money(15_000)), validator.parseAmount("150"))
        assertEquals(AmountParseResult.Valid(Money(15_050)), validator.parseAmount("150,50"))
        assertEquals(AmountParseResult.Valid(Money(15_050)), validator.parseAmount("150.5"))
        assertEquals(AmountParseResult.Valid(Money(115_050)), validator.parseAmount("1 150,50"))
        assertEquals(AmountParseResult.Valid(Money(10_000)), validator.parseAmount("100 ₽"))
        assertEquals(AmountParseResult.Valid(Money(50)), validator.parseAmount("0,50"))
    }

    @Test
    fun futureDateIsRejectedInEveryForm() {
        val create = command(spentAt = today.plusDays(1))
        val edit = command(id = "existing", spentAt = today.plusDays(7))
        assertEquals(DateFieldError.FUTURE, validator.validate(create, today).date)
        assertEquals(DateFieldError.FUTURE, validator.validate(edit, today).date)
        assertNull(validator.validate(command(spentAt = today), today).date)
        assertNull(validator.validate(command(spentAt = today.minusDays(1)), today).date)
        assertNull(validator.validate(command(spentAt = null), today).date)
    }

    @Test
    fun blankNameIsRejected() {
        assertEquals(NameFieldError.EMPTY, validator.validate(command(name = "   "), today).name)
        assertEquals(NameFieldError.EMPTY, validator.validate(command(name = "..."), today).name)
        assertNull(validator.validate(command(name = "Латте"), today).name)
    }

    @Test
    fun validCommandHasNoFieldErrors() {
        val errors = validator.validate(command(), today)
        assertTrue(!errors.hasErrors)
    }

    private fun command(
        id: String? = null,
        amount: String = "150",
        spentAt: LocalDate? = today,
        name: String = "Латте",
    ) = SaveExpenseCommand(
        id = id,
        amountInput = amount,
        spentAt = spentAt,
        name = name,
        categoryId = 1,
        categoryAssignmentSource = CategoryAssignmentSource.FALLBACK,
    )
}
