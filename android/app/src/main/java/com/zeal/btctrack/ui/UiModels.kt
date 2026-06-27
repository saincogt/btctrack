package com.zeal.btctrack.ui

fun String.redactedAddress(): String =
    if (length <= 12) this else "${take(6)}...${takeLast(4)}"
