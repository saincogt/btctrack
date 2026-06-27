package com.zeal.btctrack.ui.security

interface BiometricGate {
    suspend fun authenticate(request: BiometricPromptRequest): BiometricGateResult
}

data class BiometricPromptRequest(
    val title: String,
    val subtitle: String,
    val description: String,
)

data class BiometricGateResult(
    val success: Boolean,
    val message: String,
)
