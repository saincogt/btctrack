package com.zeal.btctrack.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zeal.btctrack.domain.model.AppSettings
import java.util.concurrent.TimeUnit

class BackgroundRefreshScheduler(
    private val context: Context,
) {
    fun sync(appSettings: AppSettings) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            buildRequest(appSettings),
        )
    }

    internal fun buildRequest(appSettings: AppSettings) = buildPeriodicRequest(appSettings)

    companion object {
        const val UNIQUE_WORK_NAME = "btctrack-background-refresh"
        const val MIN_INTERVAL_MINUTES = 15L

        internal fun buildPeriodicRequest(appSettings: AppSettings): androidx.work.PeriodicWorkRequest {
            val intervalMinutes = appSettings.refreshIntervalMinutes.toLong()
                .coerceAtLeast(MIN_INTERVAL_MINUTES)
            // Flex window randomizes the actual firing time within each period
            val flexMinutes = (intervalMinutes / 5).coerceAtLeast(5L)
            return PeriodicWorkRequestBuilder<BackgroundRefreshWorker>(
                intervalMinutes, TimeUnit.MINUTES,
                flexMinutes, TimeUnit.MINUTES,
            )
                .setConstraints(defaultConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
        }

        internal fun defaultConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    }
}
