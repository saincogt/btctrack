package com.zeal.btctrack.data.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

class MempoolOnionApi(
    baseUrl: String = TorOnlyEsploraClient.DEFAULT_BASE_URL,
) {
    private val endpoint = TorOnlyEndpointPolicy.requireOnion(baseUrl)
    private val apiBaseUrl: HttpUrl = endpoint.baseUrl.toString().toHttpUrl()

    val host: String get() = endpoint.host

    fun addressRequest(address: String): Request =
        Request.Builder()
            .url(
                apiBaseUrl.newBuilder()
                    .addPathSegments("address/$address")
                    .build()
            )
            .get()
            .build()

    fun tipHeightRequest(): Request =
        Request.Builder()
            .url(
                apiBaseUrl.newBuilder()
                    .addPathSegments("blocks/tip/height")
                    .build()
            )
            .get()
            .build()
}
