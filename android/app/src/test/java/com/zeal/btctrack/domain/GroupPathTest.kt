package com.zeal.btctrack.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupPathTest {
    @Test
    fun `keeps a clean path unchanged`() {
        assertEquals("Wallet/Sub", normalizeGroupPath("Wallet/Sub"))
    }

    @Test
    fun `trims segments and drops empty ones`() {
        assertEquals("Wallet/Sub", normalizeGroupPath("  Wallet / /Sub/ "))
    }

    @Test
    fun `blank path becomes empty`() {
        assertEquals("", normalizeGroupPath("   "))
        assertEquals("", normalizeGroupPath("/"))
    }
}
