package com.zeal.btctrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zeal.btctrack.data.local.entity.BalanceSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceSnapshotDao {
    @Query("SELECT * FROM balance_snapshots ORDER BY fetched_at DESC, address ASC")
    fun observeAll(): Flow<List<BalanceSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BalanceSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BalanceSnapshotEntity>)

    @Query("SELECT * FROM balance_snapshots WHERE address = :address LIMIT 1")
    suspend fun findByAddress(address: String): BalanceSnapshotEntity?

    @Query("DELETE FROM balance_snapshots")
    suspend fun clearAll()
}
