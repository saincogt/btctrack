package com.zeal.btctrack.data.remote

import com.zeal.btctrack.domain.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit

class TorHealthChecker(
    baseUrl: String = TorOnlyEsploraClient.DEFAULT_BASE_URL,
    appSettings: AppSettings,
    httpClient: OkHttpClient? = null,
) {
    private val proxyConfig = TorProxyConfig.from(appSettings).also { it.validate() }
    private val api = MempoolOnionApi(baseUrl)
    private val client = httpClient ?: OkHttpClient.Builder()
        .proxy(TorSocksProxyFactory.from(appSettings))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun check(): TorHealthStatus = withContext(Dispatchers.IO) {
        val request = api.tipHeightRequest()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext TorHealthStatus(
                        ok = false,
                        endpointHost = api.host,
                        message = "HTTP ${response.code} via ${proxyConfig.host}:${proxyConfig.port}",
                    )
                }
                val body = response.body?.string().orEmpty().trim()
                val height = body.toLongOrNull()
                if (height == null) {
                    TorHealthStatus(
                        ok = false,
                        endpointHost = api.host,
                        message = "Unexpected tip height response via ${proxyConfig.host}:${proxyConfig.port}",
                    )
                } else {
                    TorHealthStatus(
                        ok = true,
                        endpointHost = api.host,
                        message = "Tor proxy reachable, tip height=$height",
                    )
                }
            }
        } catch (error: IOException) {
            TorHealthStatus(
                ok = false,
                endpointHost = api.host,
                message = error.message ?: error::class.java.simpleName,
            )
        }
    }
}
