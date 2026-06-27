package com.zeal.btctrack.background

import androidx.work.ListenableWorker

class BackgroundRefreshRunner(
    private val refreshAll: suspend () -> Unit,
) {
    suspend fun run(): ListenableWorker.Result =
        runCatching {
            refreshAll()
            ListenableWorker.Result.success()
        }.getOrElse {
            ListenableWorker.Result.retry()
        }
}
