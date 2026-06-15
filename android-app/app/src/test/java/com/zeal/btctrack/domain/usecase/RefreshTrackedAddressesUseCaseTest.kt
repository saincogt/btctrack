package com.zeal.btctrack.domain.usecase

import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.model.AppSettings
import com.zeal.btctrack.domain.model.BalanceSnapshot
import com.zeal.btctrack.domain.refresh.DelayStrategy
import com.zeal.btctrack.domain.refresh.JitterPlanner
import com.zeal.btctrack.domain.refresh.RefreshOrderStrategy
import com.zeal.btctrack.domain.repository.AddressRepository
import com.zeal.btctrack.domain.repository.BalanceRepository
import com.zeal.btctrack.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshTrackedAddressesUseCaseTest {
    private val addressA = AddressEntry(
        id = "1",
        address = "bc1q-a",
        label = "A",
        note = "",
        groupPath = "watch",
        order = 2,
        watchOnly = true,
        createdAt = 1L,
        updatedAt = 1L,
    )
    private val addressB = addressA.copy(id = "2", address = "bc1q-b", label = "B", order = 1)

    @Test
    fun `refreshes shuffled addresses and writes snapshots with jitter`() = runBlocking {
        val addressRepository = FakeAddressRepository(listOf(addressA, addressB))
        val balanceRepository = FakeBalanceRepository()
        val settingsRepository = FakeSettingsRepository(AppSettings(jitterMinMs = 10, jitterMaxMs = 20))
        val remoteUseCase = FakeRefreshAddressBalanceUseCase(
            mapOf(
                "bc1q-b" to snapshot("bc1q-b", 200L),
                "bc1q-a" to snapshot("bc1q-a", 100L),
            )
        )
        val delayStrategy = RecordingDelayStrategy()
        val useCase = RefreshTrackedAddressesUseCase(
            addressRepository = addressRepository,
            balanceRepository = balanceRepository,
            settingsRepository = settingsRepository,
            refreshAddressBalance = remoteUseCase::invoke,
            refreshOrderStrategy = FixedOrderStrategy(listOf(addressB, addressA)),
            jitterPlanner = FixedJitterPlanner(listOf(10L, 20L)),
            delayStrategy = delayStrategy,
        )

        val result = useCase()

        assertEquals(listOf("bc1q-b", "bc1q-a"), remoteUseCase.calls)
        assertEquals(listOf(10L, 20L), delayStrategy.recorded)
        assertEquals(2, result.snapshots.size)
        assertEquals(listOf("bc1q-b", "bc1q-a"), result.snapshots.map { it.address })
        assertEquals(listOf("bc1q-b", "bc1q-a"), balanceRepository.upserted.flatMap { it }.map { it.address })
    }

    @Test
    fun `returns empty result when there are no tracked addresses`() = runBlocking {
        val useCase = RefreshTrackedAddressesUseCase(
            addressRepository = FakeAddressRepository(emptyList()),
            settingsRepository = FakeSettingsRepository(AppSettings()),
            refreshAddressBalance = { error("should not refresh") },
            refreshOrderStrategy = FixedOrderStrategy(emptyList()),
            jitterPlanner = FixedJitterPlanner(emptyList()),
            delayStrategy = RecordingDelayStrategy(),
        )

        val result = useCase()

        assertEquals(0, result.snapshots.size)
        assertEquals(0, result.refreshedCount)
    }

    private fun snapshot(address: String, confirmedSats: Long) = BalanceSnapshot(
        address = address,
        confirmedSats = confirmedSats,
        unconfirmedSats = 0,
        txCount = 1,
        fetchedAt = 123L,
        source = "onion",
        success = true,
        errorSummary = null,
    )

    private class FakeAddressRepository(
        private val addresses: List<AddressEntry>,
    ) : AddressRepository {
        override fun observeAll(): Flow<List<AddressEntry>> = flowOf(addresses)
        override suspend fun replaceAll(entries: List<AddressEntry>) = Unit
        override suspend fun findByAddress(address: String): AddressEntry? = addresses.firstOrNull { it.address == address }
        override suspend fun add(entry: AddressEntry) = Unit
        override suspend fun update(entry: AddressEntry) = Unit
        override suspend fun delete(address: String) = Unit
    }

    private class FakeSettingsRepository(
        private val settings: AppSettings,
    ) : SettingsRepository {
        override fun observe(): Flow<AppSettings> = flowOf(settings)
        override suspend fun update(transform: (AppSettings) -> AppSettings) = Unit
    }

    private class FakeBalanceRepository : BalanceRepository {
        val upserted = mutableListOf<List<BalanceSnapshot>>()
        override fun observeAll(): Flow<List<BalanceSnapshot>> = flowOf(emptyList())
        override suspend fun upsertAll(snapshots: List<BalanceSnapshot>) {
            upserted += snapshots
        }
        override suspend fun findByAddress(address: String): BalanceSnapshot? = null
        override suspend fun clearAll() = Unit
    }

    private class FakeRefreshAddressBalanceUseCase(
        private val snapshots: Map<String, BalanceSnapshot>,
    ) {
        val calls = mutableListOf<String>()
        suspend fun invoke(address: String): BalanceSnapshot {
            calls += address
            return snapshots.getValue(address)
        }
    }

    private class RecordingDelayStrategy : DelayStrategy {
        val recorded = mutableListOf<Long>()
        override suspend fun sleep(durationMs: Long) {
            recorded += durationMs
        }
    }

    private class FixedJitterPlanner(
        private val values: List<Long>,
    ) : JitterPlanner {
        private var index = 0
        override fun nextDelayMs(minMs: Long, maxMs: Long): Long = values[index++]
    }

    private class FixedOrderStrategy(
        private val ordered: List<AddressEntry>,
    ) : RefreshOrderStrategy {
        override fun reorder(addresses: List<AddressEntry>): List<AddressEntry> = ordered
    }
}
