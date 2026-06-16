package com.zeal.btctrack.data.repository

import com.zeal.btctrack.data.local.dao.BalanceSnapshotDao
import com.zeal.btctrack.data.mapper.toDomain
import com.zeal.btctrack.data.mapper.toEntity
import com.zeal.btctrack.domain.model.BalanceSnapshot
import com.zeal.btctrack.domain.repository.BalanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BalanceRepositoryImpl(
    private val balanceSnapshotDao: BalanceSnapshotDao,
) : BalanceRepository {
    override fun observeAll(): Flow<List<BalanceSnapshot>> =
        balanceSnapshotDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertAll(snapshots: List<BalanceSnapshot>) {
        balanceSnapshotDao.upsertAll(snapshots.map { it.toEntity() })
    }

    override suspend fun findByAddress(address: String): BalanceSnapshot? =
        balanceSnapshotDao.findByAddress(address)?.toDomain()

    override suspend fun clearAll() {
        balanceSnapshotDao.clearAll()
    }
}
