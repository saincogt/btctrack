package com.zeal.btctrack.data.remote.parser

import com.zeal.btctrack.data.remote.dto.EsploraAddressDto
import com.zeal.btctrack.domain.model.BalanceSnapshot
import kotlinx.serialization.json.Json

class EsploraAddressParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parse(address: String, body: String, fetchedAt: Long, source: String): BalanceSnapshot {
        val dto = json.decodeFromString<EsploraAddressDto>(body)
        val confirmed = dto.chainStats.fundedTxoSum - dto.chainStats.spentTxoSum
        val unconfirmed = dto.mempoolStats.fundedTxoSum - dto.mempoolStats.spentTxoSum
        val txCount = dto.chainStats.txCount + dto.mempoolStats.txCount

        return BalanceSnapshot(
            address = address,
            confirmedSats = confirmed,
            unconfirmedSats = unconfirmed,
            txCount = txCount,
            fetchedAt = fetchedAt,
            source = source,
            success = true,
            errorSummary = null,
        )
    }
}
