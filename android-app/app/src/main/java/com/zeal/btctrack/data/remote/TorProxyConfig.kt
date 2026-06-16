package com.zeal.btctrack.data.remote

import com.zeal.btctrack.domain.model.AppSettings

data class TorProxyConfig(
    val host: String,
    val port: Int,
    val torRequired: Boolean,
) {
    fun validate() {
        require(host.isNotBlank()) { "SOCKS host is required." }
        require(port in 1..65_535) { "SOCKS port must be between 1 and 65535." }
        require(torRequired) { "Tor-only mode cannot run with torRequired=false." }
    }

    companion object {
        fun from(settings: AppSettings): TorProxyConfig = TorProxyConfig(
            host = settings.socksHost,
            port = settings.socksPort,
            torRequired = settings.torRequired,
        )
    }
}
