package com.zeal.btctrack.data.remote

data class TorHealthStatus(
    val ok: Boolean,
    val endpointHost: String,
    val message: String,
)
