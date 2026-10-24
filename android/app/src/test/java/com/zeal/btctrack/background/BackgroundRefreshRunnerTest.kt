package com.zeal.btctrack.background

import androidx.work.ListenableWorker
import com.zeal.btctrack.domain.model.BalanceSnapshot
import com.zeal.btctrack.domain.usecase.RefreshBatchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BackgroundRefreshRunnerTest {
    private fun snapshot(success: Boolean) = BalanceSnapshot(
        address = "bc1q", confirmedSats = 0, unconfirmedSats = 0, txCount = 0,
        fetchedAt = 0L, source = "test", success = success, errorSummary = null,
    )

    @Test
    fun `returns success when refresh completes`() = runTest {
        val result = BackgroundRefreshRunner(
            refreshAll = { RefreshBatchResult(listOf(snapshot(true)), attemptedCount = 1) }
        ).run()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `returns success when there is nothing to refresh`() = runTest {
        val result = BackgroundRefreshRunner(refreshAll = { RefreshBatchResult(emptyList()) }).run()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `returns retry when every address failed`() = runTest {
        val result = BackgroundRefreshRunner(
            refreshAll = { RefreshBatchResult(listOf(snapshot(false)), attemptedCount = 2) }
        ).run()
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `returns retry when refresh throws`() = runTest {
        val result = BackgroundRefreshRunner(
            refreshAll = { error("boom") }
        ).run()
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `propagates cancellation`() = runTest {
        try {
            BackgroundRefreshRunner(refreshAll = { throw CancellationException("stopped") }).run()
            fail("expected CancellationException")
        } catch (_: CancellationException) {
        }
    }
}
