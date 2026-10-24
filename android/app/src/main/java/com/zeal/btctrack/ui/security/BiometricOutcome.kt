package com.zeal.btctrack.ui.security

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

// Resumes the waiting caller at most once. A soft failure (unrecognized finger) is not
// an outcome: BiometricPrompt stays open so the user can retry.
internal class BiometricOutcome(
    private val continuation: CancellableContinuation<BiometricGateResult>,
) {
    fun succeeded() = finish(BiometricGateResult(success = true, message = "Authenticated"))

    fun error(message: String) = finish(BiometricGateResult(success = false, message = message))

    fun softFailure() = Unit

    private fun finish(result: BiometricGateResult) {
        if (continuation.isActive) continuation.resume(result)
    }
}
