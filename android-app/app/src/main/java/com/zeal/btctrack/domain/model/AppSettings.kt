package com.zeal.btctrack.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val torRequired: Boolean = true,
    val socksHost: String = "",
    val socksPort: Int = 9050,
    val refreshIntervalMinutes: Int = 60,
    val showTotalBalance: Boolean = true,
    val requireBiometricForDetails: Boolean = true,
    val requireBiometricForReveal: Boolean = true,
    val jitterMinMs: Long = 500,
    val jitterMaxMs: Long = 2_000,
    val esploraBaseUrl: String = "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/api/",
    val balanceUnit: String = "sats",
    val excludedGroups: Set<String> = emptySet(),
    val themeMode: String = "system",
)
