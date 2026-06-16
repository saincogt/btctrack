package com.zeal.btctrack.background

import androidx.work.ListenableWorker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundRefreshRunnerTest {
    @Test
    fun `returns success when refresh completes`() = runTest {
        val result = BackgroundRefreshRunner(refreshAll = {}).run()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `returns retry when refresh throws`() = runTest {
        val result = BackgroundRefreshRunner(
            refreshAll = { error("boom") }
        ).run()
        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
