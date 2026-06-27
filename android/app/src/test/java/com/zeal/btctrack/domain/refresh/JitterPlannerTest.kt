package com.zeal.btctrack.domain.refresh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class JitterPlannerTest {
    @Test
    fun `returns min value when bounds are equal`() {
        val planner = RandomJitterPlanner(Random(7))

        assertEquals(500L, planner.nextDelayMs(500L, 500L))
    }

    @Test
    fun `jitter stays within inclusive bounds`() {
        val planner = RandomJitterPlanner(Random(42))

        val delay = planner.nextDelayMs(500L, 2000L)

        assertTrue(delay in 500L..2000L)
    }
}
