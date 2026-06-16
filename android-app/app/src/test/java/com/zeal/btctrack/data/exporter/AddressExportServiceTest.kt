package com.zeal.btctrack.data.exporter

import com.zeal.btctrack.domain.model.AddressEntry
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressExportServiceTest {
    private val service = AddressExportService()

    @Test
    fun `exports cli json with label and note fields`() {
        val json = service.export(
            listOf(
                entry(address = "bc1q1", label = "Alpha", note = "note-a"),
                entry(address = "bc1q2", label = "Beta", note = ""),
            ),
            ExportFormat.CLI_JSON,
        )

        assertTrue(json.contains("\"address\": \"bc1q1\""))
        assertTrue(json.contains("\"label\": \"Alpha\""))
        assertTrue(json.contains("\"note\": \"note-a\""))
        assertTrue(json.contains("\"address\": \"bc1q2\""))
    }

    @Test
    fun `exports swiftbar json with group and watch only fields`() {
        val json = service.export(
            listOf(
                entry(address = "bc1q1", label = "Alpha", groupPath = "A/B", order = 2, watchOnly = true),
            ),
            ExportFormat.SWIFTBAR_JSON,
        )

        assertTrue(json.contains("\"group\": \"A/B\""))
        assertTrue(json.contains("\"watch_only\": true"))
    }

    private fun entry(
        address: String,
        label: String = "",
        note: String = "",
        groupPath: String = "",
        order: Int = 9999,
        watchOnly: Boolean = false,
    ) = AddressEntry(
        id = address,
        address = address,
        label = label,
        note = note,
        groupPath = groupPath,
        order = order,
        watchOnly = watchOnly,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
