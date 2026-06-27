package com.zeal.btctrack.domain.usecase

import com.zeal.btctrack.domain.model.BalanceSnapshot
import com.zeal.btctrack.domain.refresh.CoroutineDelayStrategy
import com.zeal.btctrack.domain.refresh.DelayStrategy
import com.zeal.btctrack.domain.refresh.JitterPlanner
import com.zeal.btctrack.domain.refresh.RandomJitterPlanner
import com.zeal.btctrack.domain.refresh.RefreshOrderStrategy
import com.zeal.btctrack.domain.refresh.ShuffleRefreshOrderStrategy
import com.zeal.btctrack.domain.repository.AddressRepository
import com.zeal.btctrack.domain.repository.BalanceRepository
import com.zeal.btctrack.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class RefreshTrackedAddressesUseCase(
    private val addressRepository: AddressRepository,
    private val settingsRepository: SettingsRepository,
    private val refreshAddressBalance: suspend (String) -> BalanceSnapshot,
    private val balanceRepository: BalanceRepository? = null,
    private val refreshOrderStrategy: RefreshOrderStrategy = ShuffleRefreshOrderStrategy(),
    private val jitterPlanner: JitterPlanner = RandomJitterPlanner(),
    private val delayStrategy: DelayStrategy = CoroutineDelayStrategy(),
) {
    suspend operator fun invoke(): RefreshBatchResult {
        val addresses = addressRepository.observeAll().first()
        if (addresses.isEmpty()) {
            return RefreshBatchResult(emptyList())
        }

        val settings = settingsRepository.observe().first()
        val ordered = refreshOrderStrategy.reorder(addresses)
        // Pre-first-request jitter so the refresh burst start time is unpredictable
        delayStrategy.sleep(jitterPlanner.nextDelayMs(settings.jitterMinMs, settings.jitterMaxMs))
        val snapshots = ordered.map { entry ->
            val snapshot = refreshAddressBalance(entry.address)
            delayStrategy.sleep(jitterPlanner.nextDelayMs(settings.jitterMinMs, settings.jitterMaxMs))
            snapshot
        }

        balanceRepository?.upsertAll(snapshots.filter { it.success })
        return RefreshBatchResult(snapshots)
    }
}

data class RefreshBatchResult(
    val snapshots: List<BalanceSnapshot>,
) {
    val refreshedCount: Int get() = snapshots.size
}
