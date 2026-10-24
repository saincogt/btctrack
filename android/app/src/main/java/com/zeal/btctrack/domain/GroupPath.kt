package com.zeal.btctrack.domain

// Canonical group path: segments trimmed, empty segments dropped ("  Wallet / /Sub/ " -> "Wallet/Sub").
fun normalizeGroupPath(path: String): String =
    path.split("/").map { it.trim() }.filter { it.isNotEmpty() }.joinToString("/")
