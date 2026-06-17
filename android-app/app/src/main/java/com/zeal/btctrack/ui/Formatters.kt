package com.zeal.btctrack.ui

import java.text.NumberFormat
import java.util.Locale

fun formatBalance(sats: Long, unit: String): String {
    val nf = NumberFormat.getNumberInstance(Locale.getDefault())
    return when (unit) {
        "BTC" -> {
            val btc = sats / 100_000_000.0
            "%.8f BTC".format(btc)
        }
        else -> "${nf.format(sats)} sats"
    }
}

fun formatRelativeTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Never"
    val diff = System.currentTimeMillis() - timestampMs
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            sdf.format(java.util.Date(timestampMs))
        }
    }
}
