package com.zeal.btctrack.data.remote

import com.zeal.btctrack.domain.model.AppSettings
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TorOnlyEsploraClientTest {
    @Test
    fun `fetch balance snapshot from onion endpoint`() = runBlocking {
        val client = TorOnlyEsploraClient(
            baseUrl = "http://mempoolhqx4isw62.onion/api/",
            appSettings = AppSettings(),
            httpClient = stubClient(body = successBody),
        )

        val snapshot = client.fetchBalanceSnapshot("bc1qtest")

        assertTrue(snapshot.success)
        assertEquals(90_000L, snapshot.confirmedSats)
        assertEquals(5_000L, snapshot.unconfirmedSats)
        assertEquals(5, snapshot.txCount)
        assertEquals("mempoolhqx4isw62.onion", snapshot.source)
    }

    @Test
    fun `returns failed snapshot when request throws`() = runBlocking {
        val client = TorOnlyEsploraClient(
            baseUrl = "http://mempoolhqx4isw62.onion/api/",
            appSettings = AppSettings(),
            httpClient = failingClient(),
        )

        val snapshot = client.fetchBalanceSnapshot("bc1qtest")

        assertFalse(snapshot.success)
        assertTrue(snapshot.errorSummary?.contains("boom") == true)
        assertEquals("mempoolhqx4isw62.onion", snapshot.source)
    }

    private fun stubClient(body: String): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody())
                    .build()
            })
            .build()

    private fun failingClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { throw IOException("boom") })
            .build()

    private val successBody = """
        {
          "chain_stats": {
            "funded_txo_sum": 100000,
            "spent_txo_sum": 10000,
            "tx_count": 4
          },
          "mempool_stats": {
            "funded_txo_sum": 5000,
            "spent_txo_sum": 0,
            "tx_count": 1
          }
        }
    """.trimIndent()
}
