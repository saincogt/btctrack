package com.zeal.btctrack.data.remote

import com.zeal.btctrack.data.remote.parser.EsploraAddressParser
import com.zeal.btctrack.domain.model.BalanceSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressStatsDtoParsingTest {
    private val parser = EsploraAddressParser()

    @Test
    fun `parses chain and mempool stats into balance snapshot`() = runBlocking {
        val snapshot: BalanceSnapshot = parser.parse(
            address = "bc1qparse",
            body = """
                {
                  "chain_stats": {
                    "funded_txo_sum": 120000,
                    "spent_txo_sum": 20000,
                    "tx_count": 3
                  },
                  "mempool_stats": {
                    "funded_txo_sum": 1000,
                    "spent_txo_sum": 500,
                    "tx_count": 1
                  }
                }
            """.trimIndent(),
            fetchedAt = 1234L,
            source = "mempoolhqx4isw62.onion",
        )

        assertTrue(snapshot.success)
        assertEquals(100000L, snapshot.confirmedSats)
        assertEquals(500L, snapshot.unconfirmedSats)
        assertEquals(4, snapshot.txCount)
        assertEquals(1234L, snapshot.fetchedAt)
    }
}
