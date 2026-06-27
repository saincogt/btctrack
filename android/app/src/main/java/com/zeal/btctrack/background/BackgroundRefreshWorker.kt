package com.zeal.btctrack.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zeal.btctrack.BtcTrackApp

class BackgroundRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? BtcTrackApp
            ?: return Result.failure()
        return BackgroundRefreshRunner { app.container.refreshAllTrackedAddresses() }.run()
    }
}
