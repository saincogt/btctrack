package com.zeal.btctrack.ui.security

class SensitiveRevealController(
    private val gate: BiometricGate,
) {
    companion object {
        fun noOpGate(): BiometricGate = object : BiometricGate {
            override suspend fun authenticate(request: BiometricPromptRequest) =
                BiometricGateResult(true, "No biometric hardware")
        }
    }
    suspend fun toggle(
        currentlyVisible: Boolean,
        requireBiometric: Boolean,
        request: BiometricPromptRequest,
    ): SensitiveRevealResult {
        if (currentlyVisible) {
            return SensitiveRevealResult(
                visible = false,
                message = "Hidden",
            )
        }

        if (!requireBiometric) {
            return SensitiveRevealResult(
                visible = true,
                message = "Revealed without biometric",
            )
        }

        val result = gate.authenticate(request)
        return SensitiveRevealResult(
            visible = result.success,
            message = result.message,
        )
    }
}

data class SensitiveRevealResult(
    val visible: Boolean,
    val message: String,
)
