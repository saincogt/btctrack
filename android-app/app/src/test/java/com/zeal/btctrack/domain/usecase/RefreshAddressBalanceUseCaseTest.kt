package com.zeal.btctrack.domain.usecase

import com.zeal.btctrack.data.remote.TorOnlyEsploraClient
import com.zeal.btctrack.domain.model.AppSettings
import com.zeal.btctrack.domain.model.BalanceSnapshot
import com.zeal.btctrack.domain.repository.BalanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshAddressBalanceUseCaseTest {
    private val repository = FakeBalanceRepository()
    private val client = TorOnlyEsploraClient(
        baseUrl = "http://mempoolhqx4isw62.onion/api/",
        appSettings = AppSettings(),
        httpClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(successBody.toResponseBody())
                    .build()
            })
            .build(),
    )
    private val useCase = RefreshAddressBalanceUseCase(client, repository)

    @Test
    fun `fetches snapshot and persists it`() = runBlocking {
        val snapshot = useCase("bc1qrefresh")

        assertTrue(snapshot.success)
        assertEquals(1, repository.stored.size)
        assertEquals("bc1qrefresh", repository.stored.first().address)
        assertEquals(42_000L, repository.stored.first().confirmedSats)
    }

    private class FakeBalanceRepository : BalanceRepository {
        var stored: List<BalanceSnapshot> = emptyList()

        override fun observeAll(): Flow<List<BalanceSnapshot>> = flowOf(stored)

        override suspend fun upsertAll(snapshots: List<BalanceSnapshot>) {
            stored = snapshots
        }

        override suspend fun findByAddress(address: String): BalanceSnapshot? =
            stored.firstOrNull { it.address == address }

        override suspend fun clearAll() {
            stored = emptyList()
        }
    }

    private val successBody = """
        {
          "chain_stats": {
            "funded_txo_sum": 50000,
            "spent_txo_sum": 8000,
            "tx_count": 2
          },
          "mempool_stats": {
            "funded_txo_sum": 0,
            "spent_txo_sum": 0,
            "tx_count": 0
          }
        }
    """.trimIndent()
}
