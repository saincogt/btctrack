package com.zeal.btctrack.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressImportServiceTest {
    private val service = AddressImportService()

    @Test
    fun `import returns entries for cli json`() {
        val json = """
            [
              {
                "address": "1BoatSLRHtKNngkdXEeobR76b53LETtpyT",
                "label": "Trading account",
                "note": "OKX spot wallet"
              }
            ]
        """.trimIndent()

        val result = service.import(
            format = ImportFormat.CLI_JSON,
            inputStream = json.byteInputStream(),
            nowEpochMillis = 1234L,
        )

        assertEquals(ImportFormat.CLI_JSON, result.format)
        assertEquals(1, result.importedCount)
        assertTrue(result.warnings.isEmpty())
        assertEquals("Trading account", result.entries.single().label)
    }

    @Test
    fun `import reports duplicate warning`() {
        val json = """
            [
              { "address": "dup", "label": "one", "note": "" },
              { "address": "dup", "label": "two", "note": "" }
            ]
        """.trimIndent()

        val result = service.import(
            format = ImportFormat.CLI_JSON,
            inputStream = json.byteInputStream(),
            nowEpochMillis = 1234L,
        )

        assertTrue(result.warnings.any { it.contains("duplicate", ignoreCase = true) })
    }
}
