package com.zeal.btctrack.ui

import java.text.NumberFormat
import java.util.Locale

fun formatBalance(sats: Long, unit: String): String {
    return when (unit) {
        "BTC" -> {
            val nf = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 8
            }
            "${nf.format(sats / 100_000_000.0)} BTC"
        }
        else -> {
            val nf = NumberFormat.getNumberInstance(Locale.getDefault())
            "${nf.format(sats)} sats"
        }
    }
}

fun formatBalanceAmount(sats: Long, unit: String): String = when (unit) {
    "BTC" -> {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 8
        }
        nf.format(sats / 100_000_000.0)
    }
    else -> NumberFormat.getNumberInstance(Locale.getDefault()).format(sats)
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
