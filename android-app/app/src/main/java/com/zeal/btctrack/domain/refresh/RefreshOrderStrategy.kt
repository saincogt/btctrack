package com.zeal.btctrack.domain.refresh

import com.zeal.btctrack.domain.model.AddressEntry

interface RefreshOrderStrategy {
    fun reorder(addresses: List<AddressEntry>): List<AddressEntry>
}
