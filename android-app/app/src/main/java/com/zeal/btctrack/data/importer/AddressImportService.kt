package com.zeal.btctrack.data.importer

import com.zeal.btctrack.data.mapper.CliAddressJsonMapper
import com.zeal.btctrack.data.mapper.SwiftBarAddressJsonMapper
import com.zeal.btctrack.domain.model.AddressEntry
import java.io.InputStream

class AddressImportService(
    private val cliMapper: CliAddressJsonMapper = CliAddressJsonMapper(),
    private val swiftBarMapper: SwiftBarAddressJsonMapper = SwiftBarAddressJsonMapper(),
) {
    fun import(
        format: ImportFormat,
        inputStream: InputStream,
        nowEpochMillis: Long,
    ): ImportResult {
        val entries = when (format) {
            ImportFormat.CLI_JSON -> cliMapper.parse(inputStream, nowEpochMillis)
            ImportFormat.SWIFTBAR_JSON -> swiftBarMapper.parse(inputStream, nowEpochMillis)
        }

        return ImportResult(
            format = format,
            importedCount = entries.size,
            warnings = buildWarnings(entries),
            entries = entries,
        )
    }

    private fun buildWarnings(entries: List<AddressEntry>): List<String> {
        val warnings = mutableListOf<String>()
        if (entries.any { it.address.isBlank() }) {
            warnings += "One or more imported entries have a blank address."
        }
        val duplicateCount = entries.groupingBy { it.address }.eachCount().count { it.value > 1 }
        if (duplicateCount > 0) {
            warnings += "Imported data contains duplicate addresses."
        }
        return warnings
    }
}

data class ImportResult(
    val format: ImportFormat,
    val importedCount: Int,
    val warnings: List<String>,
    val entries: List<AddressEntry>,
)
