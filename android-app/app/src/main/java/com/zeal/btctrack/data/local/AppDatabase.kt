package com.zeal.btctrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zeal.btctrack.data.local.dao.AddressDao
import com.zeal.btctrack.data.local.dao.BalanceSnapshotDao
import com.zeal.btctrack.data.local.entity.AddressEntity
import com.zeal.btctrack.data.local.entity.BalanceSnapshotEntity

@Database(
    entities = [AddressEntity::class, BalanceSnapshotEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun addressDao(): AddressDao
    abstract fun balanceSnapshotDao(): BalanceSnapshotDao
}
