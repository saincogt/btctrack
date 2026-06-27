package com.zeal.btctrack.ui.security

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveRevealControllerTest {
    @Test
    fun `hide action does not require biometric`() = runTest {
        val controller = SensitiveRevealController(FakeBiometricGate())

        val result = controller.toggle(
            currentlyVisible = true,
            requireBiometric = true,
            request = sampleRequest(),
        )

        assertFalse(result.visible)
        assertEquals("Hidden", result.message)
    }

    @Test
    fun `reveal succeeds without biometric when disabled`() = runTest {
        val controller = SensitiveRevealController(FakeBiometricGate())

        val result = controller.toggle(
            currentlyVisible = false,
            requireBiometric = false,
            request = sampleRequest(),
        )

        assertTrue(result.visible)
        assertEquals("Revealed without biometric", result.message)
    }

    @Test
    fun `reveal delegates to biometric gate when required`() = runTest {
        val controller = SensitiveRevealController(
            FakeBiometricGate(BiometricGateResult(success = true, message = "Authenticated"))
        )

        val result = controller.toggle(
            currentlyVisible = false,
            requireBiometric = true,
            request = sampleRequest(),
        )

        assertTrue(result.visible)
        assertEquals("Authenticated", result.message)
    }

    @Test
    fun `failed biometric keeps value hidden`() = runTest {
        val controller = SensitiveRevealController(
            FakeBiometricGate(BiometricGateResult(success = false, message = "No match"))
        )

        val result = controller.toggle(
            currentlyVisible = false,
            requireBiometric = true,
            request = sampleRequest(),
        )

        assertFalse(result.visible)
        assertEquals("No match", result.message)
    }

    private fun sampleRequest() = BiometricPromptRequest(
        title = "Reveal",
        subtitle = "Sensitive data",
        description = "Confirm biometric",
    )
}

private class FakeBiometricGate(
    private val result: BiometricGateResult = BiometricGateResult(success = true, message = "Authenticated"),
) : BiometricGate {
    override suspend fun authenticate(request: BiometricPromptRequest): BiometricGateResult = result
}
