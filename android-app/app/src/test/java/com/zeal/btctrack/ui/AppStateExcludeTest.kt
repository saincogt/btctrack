package com.zeal.btctrack.ui

import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.model.BalanceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStateExcludeTest {

    private fun entry(address: String, groupPath: String) = AddressEntry(
        id = address, address = address, label = "", note = "",
        groupPath = groupPath, order = 0, watchOnly = false,
        createdAt = 0L, updatedAt = 0L,
    )

    private fun snap(address: String, sats: Long) = BalanceSnapshot(
        address = address, confirmedSats = sats, unconfirmedSats = 0L,
        txCount = 0, fetchedAt = 1000L, source = "test",
        success = true, errorSummary = null,
    )

    @Test
    fun `exact match excluded`() {
        val state = buildDashboardState(
            addresses = listOf(entry("a1", "Cold"), entry("a2", "Exchange")),
            balances = listOf(snap("a1", 100L), snap("a2", 50L)),
            torStatus = "", showBalance = true,
            excludedGroups = setOf("Cold"),
        )
        assertEquals(50L, state.totalBalanceSats)
    }

    @Test
    fun `parent path excludes child paths`() {
        val state = buildDashboardState(
            addresses = listOf(
                entry("a1", "Cold"),
                entry("a2", "Cold/Alex"),
                entry("a3", "Exchange"),
            ),
            balances = listOf(snap("a1", 100L), snap("a2", 200L), snap("a3", 50L)),
            torStatus = "", showBalance = true,
            excludedGroups = setOf("Cold"),
        )
        assertEquals(50L, state.totalBalanceSats)
    }

    @Test
    fun `child path does not exclude sibling or parent`() {
        val state = buildDashboardState(
            addresses = listOf(
                entry("a1", "Cold"),
                entry("a2", "Cold/Alex"),
                entry("a3", "Cold/Bob"),
            ),
            balances = listOf(snap("a1", 100L), snap("a2", 200L), snap("a3", 50L)),
            torStatus = "", showBalance = true,
            excludedGroups = setOf("Cold/Alex"),
        )
        assertEquals(150L, state.totalBalanceSats)
    }

    @Test
    fun `blank groupPath address included when only named paths excluded`() {
        val state = buildDashboardState(
            addresses = listOf(entry("a1", ""), entry("a2", "Cold")),
            balances = listOf(snap("a1", 100L), snap("a2", 50L)),
            torStatus = "", showBalance = true,
            excludedGroups = setOf("Cold"),
        )
        assertEquals(100L, state.totalBalanceSats)
    }

    @Test
    fun `no excluded groups includes all`() {
        val state = buildDashboardState(
            addresses = listOf(entry("a1", "Cold"), entry("a2", "Exchange")),
            balances = listOf(snap("a1", 100L), snap("a2", 50L)),
            torStatus = "", showBalance = true,
            excludedGroups = emptySet(),
        )
        assertEquals(150L, state.totalBalanceSats)
    }
}
