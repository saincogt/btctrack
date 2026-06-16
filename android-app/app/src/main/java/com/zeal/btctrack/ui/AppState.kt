package com.zeal.btctrack.ui

import com.zeal.btctrack.data.importer.ImportFormat
import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.model.AppSettings
import com.zeal.btctrack.domain.model.BalanceSnapshot

data class DashboardUiState(
    val torStatus: String = "Tor status unknown",
    val trackedCount: Int = 0,
    val lastRefreshLabel: String = "Not available",
    val totalBalanceSats: Long = 0,
    val showBalance: Boolean = false,
)

data class ImportUiState(
    val rawJson: String = "",
    val format: ImportFormat = ImportFormat.CLI_JSON,
    val resultMessage: String = "Paste CLI JSON or SwiftBar JSON to import.",
)

data class SettingsFormState(
    val socksHost: String = "127.0.0.1",
    val socksPort: String = "9050",
    val refreshIntervalMinutes: String = "60",
    val jitterMinMs: String = "500",
    val jitterMaxMs: String = "2000",
    val torRequired: Boolean = true,
    val showTotalBalance: Boolean = true,
    val requireBiometricForDetails: Boolean = true,
    val requireBiometricForReveal: Boolean = true,
    val esploraBaseUrl: String = "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/api/",
) {
    companion object {
        fun from(settings: AppSettings) = SettingsFormState(
            socksHost = settings.socksHost,
            socksPort = settings.socksPort.toString(),
            refreshIntervalMinutes = settings.refreshIntervalMinutes.toString(),
            jitterMinMs = settings.jitterMinMs.toString(),
            jitterMaxMs = settings.jitterMaxMs.toString(),
            torRequired = settings.torRequired,
            showTotalBalance = settings.showTotalBalance,
            requireBiometricForDetails = settings.requireBiometricForDetails,
            requireBiometricForReveal = settings.requireBiometricForReveal,
            esploraBaseUrl = settings.esploraBaseUrl,
        )
    }
}

fun buildDashboardState(
    addresses: List<AddressEntry>,
    balances: List<BalanceSnapshot>,
    torStatus: String,
    showBalance: Boolean,
): DashboardUiState {
    val lastRefreshAt = balances.maxOfOrNull { it.fetchedAt }
    return DashboardUiState(
        torStatus = torStatus,
        trackedCount = addresses.size,
        lastRefreshLabel = lastRefreshAt?.toString() ?: "Not available",
        totalBalanceSats = balances.filter { it.success }.sumOf { it.confirmedSats + it.unconfirmedSats },
        showBalance = showBalance,
    )
}
