package com.zeal.btctrack.domain.refresh

import kotlinx.coroutines.delay

class CoroutineDelayStrategy : DelayStrategy {
    override suspend fun sleep(durationMs: Long) {
        delay(durationMs)
    }
}
