package com.zeal.btctrack.data.remote

import java.net.URI
import java.net.URL

data class EndpointSource(
    val baseUrl: URL,
    val host: String,
    val isOnion: Boolean,
)

object TorOnlyEndpointPolicy {
    fun requireOnion(baseUrl: String): EndpointSource {
        val normalized = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
        val url = URI(normalized).toURL()
        val host = url.host.lowercase()
        require(host.endsWith(".onion")) {
            "Tor-only mode requires an onion endpoint. Refusing non-onion host: $host"
        }
        return EndpointSource(baseUrl = url, host = host, isOnion = true)
    }
}
