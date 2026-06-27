package com.zeal.btctrack.background

import androidx.work.NetworkType
import com.zeal.btctrack.domain.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundRefreshSchedulerTest {
    @Test
    fun `build request respects minimum interval and constraints`() {
        val request = BackgroundRefreshScheduler.buildPeriodicRequest(
            AppSettings(refreshIntervalMinutes = 5)
        )

        assertEquals(BackgroundRefreshScheduler.MIN_INTERVAL_MINUTES * 60_000L, request.workSpec.intervalDuration)
        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertTrue(request.workSpec.constraints.requiresBatteryNotLow())
    }

    @Test
    fun `build request keeps configured interval when above minimum`() {
        val request = BackgroundRefreshScheduler.buildPeriodicRequest(
            AppSettings(refreshIntervalMinutes = 120)
        )

        assertEquals(120L * 60_000L, request.workSpec.intervalDuration)
    }
}
