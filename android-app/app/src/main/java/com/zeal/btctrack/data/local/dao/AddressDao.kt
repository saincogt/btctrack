package com.zeal.btctrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zeal.btctrack.data.local.entity.AddressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses ORDER BY order_value ASC, updated_at ASC, address ASC")
    fun observeAll(): Flow<List<AddressEntity>>

    @Query("SELECT * FROM addresses ORDER BY order_value ASC, updated_at ASC, address ASC")
    suspend fun getAll(): List<AddressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AddressEntity>)

    @Query("DELETE FROM addresses")
    suspend fun clearAll()

    @Query("SELECT * FROM addresses WHERE address = :address LIMIT 1")
    suspend fun findByAddress(address: String): AddressEntity?

    @Query("DELETE FROM addresses WHERE address = :address")
    suspend fun deleteByAddress(address: String)
}
