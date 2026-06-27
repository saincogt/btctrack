package com.zeal.btctrack.data.repository

import androidx.test.core.app.ApplicationProvider
import com.zeal.btctrack.data.local.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryImplTest {
    @Test
    fun `save updates settings and reloads them`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = SettingsStore(context, "test-settings-${UUID.randomUUID()}")
        val repository = SettingsRepositoryImpl(store)

        val initial = repository.observe().first()
        assertTrue(initial.torRequired)
        assertEquals(9050, initial.socksPort)

        repository.update {
            it.copy(
                socksHost = "10.0.0.2",
                socksPort = 9150,
                refreshIntervalMinutes = 180,
                showTotalBalance = false,
            )
        }

        val updated = repository.observe().first()
        assertEquals("10.0.0.2", updated.socksHost)
        assertEquals(9150, updated.socksPort)
        assertEquals(180, updated.refreshIntervalMinutes)
        assertEquals(false, updated.showTotalBalance)
    }
}
