package com.zeal.btctrack.domain.usecase

import com.zeal.btctrack.data.importer.AddressImportService
import com.zeal.btctrack.data.importer.ImportFormat
import com.zeal.btctrack.domain.normalizeGroupPath
import com.zeal.btctrack.domain.repository.AddressRepository
import com.zeal.btctrack.domain.repository.BalanceRepository
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class ImportAddressesUseCase(
    private val importService: AddressImportService,
    private val addressRepository: AddressRepository,
    private val balanceRepository: BalanceRepository? = null,
) {
    suspend operator fun invoke(
        rawJson: String,
        format: ImportFormat,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ) = importService.import(
        format = format,
        inputStream = ByteArrayInputStream(rawJson.toByteArray(StandardCharsets.UTF_8)),
        nowEpochMillis = nowEpochMillis,
    ).let { result ->
        result.copy(entries = result.entries.map { it.copy(groupPath = normalizeGroupPath(it.groupPath)) })
    }.also { result ->
        addressRepository.replaceAll(result.entries)
        // Drop balances of addresses the import removed so they stop affecting "Last refresh"
        balanceRepository?.deleteOrphans()
    }
}
