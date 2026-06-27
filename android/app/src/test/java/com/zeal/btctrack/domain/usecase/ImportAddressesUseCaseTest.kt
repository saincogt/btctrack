package com.zeal.btctrack.domain.usecase

import com.zeal.btctrack.data.importer.AddressImportService
import com.zeal.btctrack.data.importer.ImportFormat
import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.repository.AddressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportAddressesUseCaseTest {
    private val repository = FakeAddressRepository()
    private val useCase = ImportAddressesUseCase(
        importService = AddressImportService(),
        addressRepository = repository,
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
