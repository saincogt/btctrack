package com.zeal.btctrack.domain.refresh

interface DelayStrategy {
    suspend fun sleep(durationMs: Long)
}
