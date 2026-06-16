package com.zeal.btctrack.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwiftBarAddressJsonMapperTest {
    private val mapper = SwiftBarAddressJsonMapper()

    @Test
    fun `parse maps swiftbar json into internal address entries`() {
        val now = 1_700_000_000_000L
        val json = """
            [
              {
                "address": "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
                "label": "Cold Storage",
                "group": "Trezor/Personal",
                "order": 1
              },
              {
                "address": "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
                "label": "Watch-only",
                "group": "Watching",
                "watch_only": true
              }
            ]
        """.trimIndent()

        val result = mapper.parse(json.byteInputStream(), now)

        assertEquals(2, result.size)
        assertEquals("Cold Storage", result[0].label)
        assertEquals("Trezor/Personal", result[0].groupPath)
        assertEquals(1, result[0].order)
        assertFalse(result[0].watchOnly)
        assertEquals("", result[0].note)

        assertEquals("Watching", result[1].groupPath)
        assertEquals(9_999, result[1].order)
        assertTrue(result[1].watchOnly)
        assertEquals(now, result[1].createdAt)
        assertEquals(now, result[1].updatedAt)
    }
}
