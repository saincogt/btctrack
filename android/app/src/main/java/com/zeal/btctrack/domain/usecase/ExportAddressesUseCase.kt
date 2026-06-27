package com.zeal.btctrack.domain.usecase

import com.zeal.btctrack.data.exporter.AddressExportService
import com.zeal.btctrack.data.exporter.ExportFormat
import com.zeal.btctrack.domain.repository.AddressRepository
import kotlinx.coroutines.flow.first

class ExportAddressesUseCase(
    private val addressRepository: AddressRepository,
    private val exportService: AddressExportService = AddressExportService(),
) {
    suspend operator fun invoke(format: ExportFormat): String {
        val entries = addressRepository.observeAll().first()
        return exportService.export(entries, format)
    }
}
