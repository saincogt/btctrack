package com.zeal.btctrack.data.exporter

import com.zeal.btctrack.data.mapper.dto.CliAddressDto
import com.zeal.btctrack.data.mapper.dto.SwiftBarAddressDto
import com.zeal.btctrack.domain.model.AddressEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AddressExportService(
    private val json: Json = Json {
        prettyPrint = true
        explicitNulls = false
        encodeDefaults = false
    },
) {
    fun export(entries: List<AddressEntry>, format: ExportFormat): String = when (format) {
        ExportFormat.CLI_JSON -> json.encodeToString(entries.toCliDtos())
        ExportFormat.SWIFTBAR_JSON -> json.encodeToString(entries.toSwiftBarDtos())
    }

    private fun List<AddressEntry>.toCliDtos(): List<CliAddressDto> =
        sortedBy { it.label.ifBlank { it.address } }
            .map { entry ->
                CliAddressDto(
                    address = entry.address,
                    label = entry.label.ifBlank { null },
                    note = entry.note.ifBlank { null },
                )
            }

    private fun List<AddressEntry>.toSwiftBarDtos(): List<SwiftBarAddressDto> =
        sortedWith(compareBy<AddressEntry> { it.groupPath }.thenBy { it.order }.thenBy { it.label }.thenBy { it.address })
            .map { entry ->
                SwiftBarAddressDto(
                    address = entry.address,
                    label = entry.label.ifBlank { null },
                    groupPath = entry.groupPath.ifBlank { null },
                    order = entry.order,
                    watchOnly = entry.watchOnly,
                )
            }
}
