package com.zeal.btctrack.domain.refresh

interface JitterPlanner {
    fun nextDelayMs(minMs: Long, maxMs: Long): Long
}
