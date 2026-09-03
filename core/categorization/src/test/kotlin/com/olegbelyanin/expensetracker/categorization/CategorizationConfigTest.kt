package com.olegbelyanin.expensetracker.categorization

import org.junit.Assert.assertEquals
import org.junit.Test

class CategorizationConfigTest {
    @Test
    fun fromJsonReadsKnownKeysAndKeepsDefaults() {
        val config = CategorizationConfig.fromJson(
            """
            {
              "MIN_SEED_SUPPORT": 3,
              "MIN_SEED_PROBABILITY": 0.75,
              "MAX_SEED_STRENGTH": 100,
              "NAME_WEIGHT": 3.0,
              "LOCATION_WEIGHT": 0.5,
              "LAPLACE_ALPHA": 0.1
            }
            """.trimIndent(),
        )
        assertEquals(3, config.minSeedSupport)
        assertEquals(0.75, config.minSeedProbability, 1e-9)
        assertEquals(100.0, config.maxSeedStrength, 1e-9)
        assertEquals(3.0, config.nameWeight, 1e-9)
        assertEquals(0.5, config.locationWeight, 1e-9)
        assertEquals(0.1, config.laplaceAlpha, 1e-9)
        assertEquals(CategorizationConfig.DEFAULT.transitionMargin, config.transitionMargin, 1e-9)
    }

    @Test
    fun fromJsonAcceptsScientificEpsilon() {
        val config = CategorizationConfig.fromJson("""{"TRANSITION_EPSILON": 1.0e-9}""")
        assertEquals(1e-9, config.transitionEpsilon, 1e-15)
    }
}
