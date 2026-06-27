package com.zeal.btctrack.data.remote

import com.zeal.btctrack.data.remote.parser.EsploraAddressParser
import com.zeal.btctrack.domain.model.AppSettings
import com.zeal.btctrack.domain.model.BalanceSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class TorOnlyEsploraClient(
    baseUrl: String = DEFAULT_BASE_URL,
    appSettings: AppSettings,
    private val parser: EsploraAddressParser = EsploraAddressParser(),
    httpClient: OkHttpClient? = null,
) {
    private val endpoint = TorOnlyEndpointPolicy.requireOnion(baseUrl)
    private val client = httpClient ?: OkHttpClient.Builder()
        .apply { TorSocksProxyFactory.from(appSettings)?.let { proxy(it) } }
        .addNetworkInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().removeHeader("User-Agent").build())
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val apiBaseUrl = endpoint.baseUrl.toString().toHttpUrl()

    suspend fun fetchBalanceSnapshot(
        address: String,
        fetchedAt: Long = System.currentTimeMillis(),
    ): BalanceSnapshot = withContext(Dispatchers.IO) {
        val url = apiBaseUrl.newBuilder()
            .addPathSegments("address/$address")
            .build()
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext failureSnapshot(
                        address = address,
                        fetchedAt = fetchedAt,
                        message = "HTTP ${response.code}",
                    )
                }
                parser.parse(
                    address = address,
                    body = body,
                    fetchedAt = fetchedAt,
                    source = endpoint.host,
                )
            }
        } catch (error: IOException) {
            failureSnapshot(address, fetchedAt, error.message ?: error::class.java.simpleName)
        }
    }

    private fun failureSnapshot(address: String, fetchedAt: Long, message: String): BalanceSnapshot =
        BalanceSnapshot(
            address = address,
            confirmedSats = 0,
            unconfirmedSats = 0,
            txCount = 0,
            fetchedAt = fetchedAt,
            source = endpoint.host,
            success = false,
            errorSummary = message,
        )

    companion object {
        const val DEFAULT_BASE_URL = "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/api/"
    }
}
