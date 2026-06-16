package com.zeal.btctrack.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CliAddressJsonMapperTest {
    private val mapper = CliAddressJsonMapper()

    @Test
    fun `parse maps cli json into internal address entries`() {
        val now = 1_700_000_000_000L
        val json = """
            [
              {
                "address": "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
                "label": "Cold storage",
                "note": "Hardware wallet - Ledger"
              },
              {
                "address": "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                "label": "",
                "note": ""
              }
            ]
        """.trimIndent()

        val result = mapper.parse(json.byteInputStream(), now)

        assertEquals(2, result.size)
        assertEquals("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa", result[0].address)
        assertEquals("Cold storage", result[0].label)
        assertEquals("Hardware wallet - Ledger", result[0].note)
        assertEquals("", result[0].groupPath)
        assertEquals(9_999, result[0].order)
        assertFalse(result[0].watchOnly)
        assertEquals(now, result[0].createdAt)
        assertEquals(now, result[0].updatedAt)
        assertTrue(result[0].id.isNotBlank())

        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", result[1].address)
        assertEquals("", result[1].label)
        assertEquals("", result[1].note)
    }
}
