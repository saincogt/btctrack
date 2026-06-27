package com.zeal.btctrack.domain.refresh

import com.zeal.btctrack.domain.model.AddressEntry
import kotlin.random.Random

class ShuffleRefreshOrderStrategy(
    private val random: Random = Random.Default,
) : RefreshOrderStrategy {
    override fun reorder(addresses: List<AddressEntry>): List<AddressEntry> =
        addresses.shuffled(random)
}
