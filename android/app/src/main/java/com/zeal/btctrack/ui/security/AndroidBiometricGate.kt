package com.zeal.btctrack.ui.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidBiometricGate(
    private val activity: FragmentActivity,
) : BiometricGate {
    override suspend fun authenticate(request: BiometricPromptRequest): BiometricGateResult =
        suspendCancellableCoroutine { continuation ->
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            val biometricManager = BiometricManager.from(activity)
            val availability = biometricManager.canAuthenticate(authenticators)
            if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
                continuation.resume(
                    BiometricGateResult(
                        success = false,
                        message = "Biometric unavailable ($availability)",
                    )
                )
                return@suspendCancellableCoroutine
            }

            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        continuation.resume(BiometricGateResult(success = true, message = "Authenticated"))
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuation.isActive) {
                            continuation.resume(
                                BiometricGateResult(
                                    success = false,
                                    message = errString.toString(),
                                )
                            )
                        }
                    }

                    override fun onAuthenticationFailed() {
                        if (continuation.isActive) {
                            continuation.resume(
                                BiometricGateResult(
                                    success = false,
                                    message = "Authentication failed",
                                )
                            )
                        }
                    }
                },
            )

            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(request.title)
                    .setSubtitle(request.subtitle)
                    .setDescription(request.description)
                    .setAllowedAuthenticators(authenticators)
                    .build()
            )
        }
}

fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
