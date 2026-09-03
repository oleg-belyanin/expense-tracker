package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.ui.components.KeypadKey
import com.olegbelyanin.expensetracker.ui.expense.AmountKeypadInput
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountKeypadInputTest {
    @Test
    fun appendsDigitsAndRejectsThirdFraction() {
        var value = ""
        value = AmountKeypadInput.apply(value, KeypadKey.Digit('1'))
        value = AmountKeypadInput.apply(value, KeypadKey.Digit('8'))
        value = AmountKeypadInput.apply(value, KeypadKey.Digit('4'))
        value = AmountKeypadInput.apply(value, KeypadKey.Digit('0'))
        value = AmountKeypadInput.apply(value, KeypadKey.Comma)
        value = AmountKeypadInput.apply(value, KeypadKey.Digit('5'))
        value = AmountKeypadInput.apply(value, KeypadKey.Digit('0'))
        value = AmountKeypadInput.apply(value, KeypadKey.Digit('9'))
        assertEquals("1840,50", value)
    }

    @Test
    fun replacesLeadingZeroAndBackspaces() {
        var value = AmountKeypadInput.apply("", KeypadKey.Digit('0'))
        value = AmountKeypadInput.apply(value, KeypadKey.Digit('7'))
        assertEquals("7", value)
        value = AmountKeypadInput.apply(value, KeypadKey.Backspace)
        assertEquals("", value)
    }
}
