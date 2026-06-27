package com.zeal.btctrack.domain.repository

import com.zeal.btctrack.domain.model.BalanceSnapshot
import kotlinx.coroutines.flow.Flow

interface BalanceRepository {
    fun observeAll(): Flow<List<BalanceSnapshot>>
    suspend fun upsertAll(snapshots: List<BalanceSnapshot>)
    suspend fun findByAddress(address: String): BalanceSnapshot?
    suspend fun clearAll()
}
