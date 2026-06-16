package com.zeal.btctrack.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BalanceSnapshot(
    val address: String,
    val confirmedSats: Long,
    val unconfirmedSats: Long,
    val txCount: Int,
    val fetchedAt: Long,
    val source: String,
    val success: Boolean,
    val errorSummary: String?,
)
