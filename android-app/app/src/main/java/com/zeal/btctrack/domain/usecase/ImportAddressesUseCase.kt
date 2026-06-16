package com.zeal.btctrack.domain.usecase

import com.zeal.btctrack.data.importer.AddressImportService
import com.zeal.btctrack.data.importer.ImportFormat
import com.zeal.btctrack.domain.repository.AddressRepository
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class ImportAddressesUseCase(
    private val importService: AddressImportService,
    private val addressRepository: AddressRepository,
) {
    suspend operator fun invoke(
        rawJson: String,
        format: ImportFormat,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ) = importService.import(
        format = format,
        inputStream = ByteArrayInputStream(rawJson.toByteArray(StandardCharsets.UTF_8)),
        nowEpochMillis = nowEpochMillis,
    ).also { result ->
            addressRepository.replaceAll(result.entries)
        }
}
