package com.olegbelyanin.expensetracker.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMigrationsTest {
    @Test
    fun versionOneExportsSchemaAndHasNoDestructiveFallbackHook() {
        assertEquals(1, AppDatabase.SCHEMA_VERSION)
        assertTrue(AppMigrations.all.isEmpty())
    }
}
