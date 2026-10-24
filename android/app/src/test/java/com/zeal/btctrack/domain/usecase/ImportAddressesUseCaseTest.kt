package com.zeal.btctrack.domain.usecase

import com.zeal.btctrack.data.importer.AddressImportService
import com.zeal.btctrack.data.importer.ImportFormat
import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.model.BalanceSnapshot
import com.zeal.btctrack.domain.repository.AddressRepository
import com.zeal.btctrack.domain.repository.BalanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportAddressesUseCaseTest {
    private val repository = FakeAddressRepository()
    private val balanceRepository = FakeBalanceRepository()
    private val useCase = ImportAddressesUseCase(
        importService = AddressImportService(),
        addressRepository = repository,
        balanceRepository = balanceRepository,
    )

    @Test
    fun `imports cli json and persists normalized entries`() = runBlocking {
        val result = useCase(
            rawJson = """
                [
                  {"address":"bc1qphase2test0000000000000000000000000001","label":"Cold","note":"CLI"}
                ]
            """.trimIndent(),
            format = ImportFormat.CLI_JSON,
            nowEpochMillis = 1_717_171_717_000,
        )

        assertEquals(1, result.importedCount)
        assertEquals(1, repository.stored.size)
        assertEquals("Cold", repository.stored.first().label)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `normalizes group paths and prunes stale balances`() = runBlocking {
        val result = useCase(
            rawJson = """
                [
                  {"address":"bc1qphase2test0000000000000000000000000001","label":"Cold","group":" Wallet / Sub/ "}
                ]
            """.trimIndent(),
            format = ImportFormat.SWIFTBAR_JSON,
            nowEpochMillis = 1_717_171_717_000,
        )

        assertEquals("Wallet/Sub", repository.stored.first().groupPath)
        assertEquals("Wallet/Sub", result.entries.first().groupPath)
        assertEquals(1, balanceRepository.orphanPrunes)
    }

    private class FakeBalanceRepository : BalanceRepository {
        var orphanPrunes = 0
        override fun observeAll(): Flow<List<BalanceSnapshot>> = flowOf(emptyList())
        override suspend fun upsertAll(snapshots: List<BalanceSnapshot>) = Unit
        override suspend fun findByAddress(address: String): BalanceSnapshot? = null
        override suspend fun clearAll() = Unit
        override suspend fun deleteOrphans() {
            orphanPrunes++
        }
    }

    private class FakeAddressRepository : AddressRepository {
        var stored: List<AddressEntry> = emptyList()

        override fun observeAll(): Flow<List<AddressEntry>> = flowOf(stored)

        override suspend fun replaceAll(entries: List<AddressEntry>) {
            stored = entries
        }

        override suspend fun findByAddress(address: String): AddressEntry? =
            stored.firstOrNull { it.address == address }

        override suspend fun add(entry: AddressEntry) {}

        override suspend fun update(entry: AddressEntry) {}

        override suspend fun delete(address: String) {}
    }
}
