package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.data.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceTest {
    @Test
    fun fromStorageReadsKnownValues() {
        assertEquals(ThemePreference.System, ThemePreference.fromStorage("system"))
        assertEquals(ThemePreference.Light, ThemePreference.fromStorage("light"))
        assertEquals(ThemePreference.Dark, ThemePreference.fromStorage("dark"))
    }

    @Test
    fun fromStorageFallsBackToSystem() {
        assertEquals(ThemePreference.System, ThemePreference.fromStorage(null))
        assertEquals(ThemePreference.System, ThemePreference.fromStorage("unknown"))
    }
}
