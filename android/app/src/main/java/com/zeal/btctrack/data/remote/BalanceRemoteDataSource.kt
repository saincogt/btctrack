package com.zeal.btctrack.data.remote

import com.zeal.btctrack.domain.model.BalanceSnapshot

class BalanceRemoteDataSource(
    private val client: TorOnlyEsploraClient,
) {
    suspend fun fetch(address: String): BalanceSnapshot = client.fetchBalanceSnapshot(address)
}
