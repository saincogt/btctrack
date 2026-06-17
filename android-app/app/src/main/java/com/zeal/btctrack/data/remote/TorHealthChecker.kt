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
    private val proxyConfig: TorProxyConfig? = if (appSettings.socksHost.isBlank()) null
        else TorProxyConfig.from(appSettings).also { it.validate() }
    private val api = MempoolOnionApi(baseUrl)
    private val client = httpClient ?: OkHttpClient.Builder()
        .apply { TorSocksProxyFactory.from(appSettings)?.let { proxy(it) } }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun check(): TorHealthStatus = withContext(Dispatchers.IO) {
        val request = api.tipHeightRequest()
        try {
            client.newCall(request).execute().use { response ->
                val via = proxyConfig?.let { " via ${it.host}:${it.port}" } ?: " (VPN mode)"
                if (!response.isSuccessful) {
                    return@withContext TorHealthStatus(
                        ok = false,
                        endpointHost = api.host,
                        message = "HTTP ${response.code}$via",
                    )
                }
                val body = response.body?.string().orEmpty().trim()
                val height = body.toLongOrNull()
                if (height == null) {
                    TorHealthStatus(
                        ok = false,
                        endpointHost = api.host,
                        message = "Unexpected tip height response$via",
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
