package com.zeal.btctrack.domain.refresh

import com.zeal.btctrack.domain.model.AddressEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class RefreshOrderStrategyTest {
    private val addresses = listOf(
        entry("bc1q-1", 1),
        entry("bc1q-2", 2),
        entry("bc1q-3", 3),
    )

    @Test
    fun `shuffle preserves all addresses and can change order`() {
        val strategy = ShuffleRefreshOrderStrategy(Random(42))

        val reordered = strategy.reorder(addresses)

        assertEquals(addresses.map { it.address }.sorted(), reordered.map { it.address }.sorted())
        assertNotEquals(addresses.map { it.address }, reordered.map { it.address })
    }

    private fun entry(address: String, order: Int) = AddressEntry(
        id = address,
        address = address,
        label = address,
        note = "",
        groupPath = "watch",
        order = order,
        watchOnly = true,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
