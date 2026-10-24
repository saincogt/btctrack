package com.zeal.btctrack.ui.security

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricAuthCallbackTest {

    @Test
    fun `soft failure keeps waiting and a later success resumes once`() = runBlocking {
        lateinit var outcome: BiometricOutcome
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            suspendCancellableCoroutine<BiometricGateResult> { outcome = BiometricOutcome(it) }
        }

        outcome.softFailure()
        assertFalse(result.isCompleted)

        outcome.succeeded()
        assertTrue(result.await().success)
    }

    @Test
    fun `outcomes after completion are ignored`() = runBlocking {
        lateinit var outcome: BiometricOutcome
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            suspendCancellableCoroutine<BiometricGateResult> { outcome = BiometricOutcome(it) }
        }

        outcome.error("Cancelled")
        outcome.succeeded()

        assertFalse(result.await().success)
    }
}
