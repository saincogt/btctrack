package com.zeal.btctrack.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EsploraStatsDto(
    @SerialName("funded_txo_sum")
    val fundedTxoSum: Long = 0,
    @SerialName("spent_txo_sum")
    val spentTxoSum: Long = 0,
    @SerialName("tx_count")
    val txCount: Int = 0,
)

@Serializable
data class EsploraAddressDto(
    @SerialName("chain_stats")
    val chainStats: EsploraStatsDto = EsploraStatsDto(),
    @SerialName("mempool_stats")
    val mempoolStats: EsploraStatsDto = EsploraStatsDto(),
)
