package com.olegbelyanin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Test

class ScaffoldSmokeTest {
    @Test
    fun jvmUnitTestPipelineUsesAppPackage() {
        assertEquals(
            "com.olegbelyanin.expensetracker",
            ScaffoldSmokeTest::class.java.packageName,
        )
    }
}
