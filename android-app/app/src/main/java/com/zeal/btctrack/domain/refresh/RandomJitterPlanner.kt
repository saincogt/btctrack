package com.zeal.btctrack.domain.refresh

import kotlin.random.Random

class RandomJitterPlanner(
    private val random: Random = Random.Default,
) : JitterPlanner {
    override fun nextDelayMs(minMs: Long, maxMs: Long): Long {
        require(minMs >= 0) { "jitterMinMs must be >= 0" }
        require(maxMs >= minMs) { "jitterMaxMs must be >= jitterMinMs" }
        if (minMs == maxMs) return minMs
        return random.nextLong(from = minMs, until = maxMs + 1)
    }
}
