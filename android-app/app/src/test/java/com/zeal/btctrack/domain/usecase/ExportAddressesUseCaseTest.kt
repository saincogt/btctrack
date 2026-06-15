package com.zeal.btctrack.domain.usecase

import com.zeal.btctrack.data.exporter.AddressExportService
import com.zeal.btctrack.data.exporter.ExportFormat
import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.repository.AddressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportAddressesUseCaseTest {
    @Test
    fun `returns cli json from repository entries`() = runTest {
        val useCase = ExportAddressesUseCase(
            addressRepository = FakeAddressRepository(
                listOf(AddressEntry("1", "bc1q1", "Label", "Note", "", 9999, false, 1, 1))
            ),
            exportService = AddressExportService(),
        )

        val json = useCase(ExportFormat.CLI_JSON)
        assertTrue(json.contains("bc1q1"))
    }

    private class FakeAddressRepository(
        private val entries: List<AddressEntry>,
    ) : AddressRepository {
        override fun observeAll(): Flow<List<AddressEntry>> = flowOf(entries)
        override suspend fun replaceAll(entries: List<AddressEntry>) {}
        override suspend fun findByAddress(address: String): AddressEntry? = entries.firstOrNull { it.address == address }
        override suspend fun add(entry: AddressEntry) {}
        override suspend fun update(entry: AddressEntry) {}
        override suspend fun delete(address: String) {}
    }
}
