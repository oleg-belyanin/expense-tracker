package com.olegbelyanin.expensetracker.ui.expense

import com.olegbelyanin.expensetracker.ui.components.KeypadKey

object AmountKeypadInput {
    fun apply(current: String, key: KeypadKey): String {
        val value = current.replace('.', ',').filterIndexed { index, char ->
            char.isDigit() || char == ',' || (index == 0 && (char == '-' || char == '−'))
        }
        return when (key) {
            is KeypadKey.Digit -> appendDigit(value, key.value)
            KeypadKey.Comma -> if (value.contains(',')) value else (value.ifEmpty { "0" } + ",")
            KeypadKey.Backspace -> value.dropLast(1)
        }
    }

    private fun appendDigit(current: String, digit: Char): String {
        if (current == "0") return digit.toString()
        val comma = current.indexOf(',')
        if (comma >= 0 && current.length - comma > 2) return current
        val integerDigits = (if (comma >= 0) current.substring(0, comma) else current).count { it.isDigit() }
        if (comma < 0 && integerDigits >= 9) return current
        return current + digit
    }
}
