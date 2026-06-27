package com.zeal.btctrack.domain.usecase

import com.zeal.btctrack.data.remote.TorOnlyEsploraClient
import com.zeal.btctrack.domain.model.BalanceSnapshot
import com.zeal.btctrack.domain.repository.BalanceRepository

class RefreshAddressBalanceUseCase(
    private val remoteClient: TorOnlyEsploraClient,
    private val balanceRepository: BalanceRepository,
) {
    suspend operator fun invoke(address: String): BalanceSnapshot {
        val snapshot = remoteClient.fetchBalanceSnapshot(address)
        balanceRepository.upsertAll(listOf(snapshot))
        return snapshot
    }
}
