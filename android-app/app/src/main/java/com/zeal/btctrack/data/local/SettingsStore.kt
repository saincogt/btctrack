package com.zeal.btctrack.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zeal.btctrack.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class SettingsStore(
    context: Context,
    fileName: String = DEFAULT_FILE_NAME,
) {
    private val resolvedFileName = if (fileName.endsWith(FILE_EXTENSION)) fileName else "$fileName.$FILE_EXTENSION"

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { File(context.filesDir, resolvedFileName) },
    )

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            torRequired = prefs[TOR_REQUIRED] ?: true,
            socksHost = prefs[SOCKS_HOST] ?: "127.0.0.1",
            socksPort = prefs[SOCKS_PORT] ?: 9050,
            refreshIntervalMinutes = prefs[REFRESH_INTERVAL_MINUTES] ?: 60,
            showTotalBalance = prefs[SHOW_TOTAL_BALANCE] ?: true,
            requireBiometricForDetails = prefs[REQUIRE_BIOMETRIC_FOR_DETAILS] ?: true,
            requireBiometricForReveal = prefs[REQUIRE_BIOMETRIC_FOR_REVEAL] ?: true,
            jitterMinMs = prefs[JITTER_MIN_MS] ?: 500L,
            jitterMaxMs = prefs[JITTER_MAX_MS] ?: 2_000L,
            esploraBaseUrl = prefs[ESPLORA_BASE_URL] ?: "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/api/",
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val updated = transform(
                AppSettings(
                    torRequired = prefs[TOR_REQUIRED] ?: true,
                    socksHost = prefs[SOCKS_HOST] ?: "127.0.0.1",
                    socksPort = prefs[SOCKS_PORT] ?: 9050,
                    refreshIntervalMinutes = prefs[REFRESH_INTERVAL_MINUTES] ?: 60,
                    showTotalBalance = prefs[SHOW_TOTAL_BALANCE] ?: true,
                    requireBiometricForDetails = prefs[REQUIRE_BIOMETRIC_FOR_DETAILS] ?: true,
                    requireBiometricForReveal = prefs[REQUIRE_BIOMETRIC_FOR_REVEAL] ?: true,
                    jitterMinMs = prefs[JITTER_MIN_MS] ?: 500L,
                    jitterMaxMs = prefs[JITTER_MAX_MS] ?: 2_000L,
                    esploraBaseUrl = prefs[ESPLORA_BASE_URL] ?: "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/api/",
                )
            )

            prefs[TOR_REQUIRED] = updated.torRequired
            prefs[SOCKS_HOST] = updated.socksHost
            prefs[SOCKS_PORT] = updated.socksPort
            prefs[REFRESH_INTERVAL_MINUTES] = updated.refreshIntervalMinutes
            prefs[SHOW_TOTAL_BALANCE] = updated.showTotalBalance
            prefs[REQUIRE_BIOMETRIC_FOR_DETAILS] = updated.requireBiometricForDetails
            prefs[REQUIRE_BIOMETRIC_FOR_REVEAL] = updated.requireBiometricForReveal
            prefs[JITTER_MIN_MS] = updated.jitterMinMs
            prefs[JITTER_MAX_MS] = updated.jitterMaxMs
            prefs[ESPLORA_BASE_URL] = updated.esploraBaseUrl
        }
    }

    companion object {
        private const val FILE_EXTENSION = "preferences_pb"
        private const val DEFAULT_FILE_NAME = "btctrack_settings.$FILE_EXTENSION"
        private val TOR_REQUIRED = booleanPreferencesKey("tor_required")
        private val SOCKS_HOST = stringPreferencesKey("socks_host")
        private val SOCKS_PORT = intPreferencesKey("socks_port")
        private val REFRESH_INTERVAL_MINUTES = intPreferencesKey("refresh_interval_minutes")
        private val SHOW_TOTAL_BALANCE = booleanPreferencesKey("show_total_balance")
        private val REQUIRE_BIOMETRIC_FOR_DETAILS = booleanPreferencesKey("require_biometric_for_details")
        private val REQUIRE_BIOMETRIC_FOR_REVEAL = booleanPreferencesKey("require_biometric_for_reveal")
        private val JITTER_MIN_MS = longPreferencesKey("jitter_min_ms")
        private val JITTER_MAX_MS = longPreferencesKey("jitter_max_ms")
        private val ESPLORA_BASE_URL = stringPreferencesKey("esplora_base_url")
    }
}
