package com.zeal.btctrack.background

import androidx.work.ListenableWorker
import com.zeal.btctrack.domain.usecase.RefreshBatchResult
import kotlinx.coroutines.CancellationException

class BackgroundRefreshRunner(
    private val refreshAll: suspend () -> RefreshBatchResult,
) {
    // Cancellation propagates so WorkManager can stop the worker; a run where every
    // address failed (e.g. Tor down) is retried with backoff.
    suspend fun run(): ListenableWorker.Result =
        try {
            if (refreshAll().allFailed) ListenableWorker.Result.retry() else ListenableWorker.Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ListenableWorker.Result.retry()
        }
}
