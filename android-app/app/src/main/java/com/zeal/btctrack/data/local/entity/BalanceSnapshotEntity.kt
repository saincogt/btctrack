package com.zeal.btctrack.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balance_snapshots")
data class BalanceSnapshotEntity(
    @PrimaryKey
    val address: String,
    @ColumnInfo(name = "confirmed_sats")
    val confirmedSats: Long,
    @ColumnInfo(name = "unconfirmed_sats")
    val unconfirmedSats: Long,
    @ColumnInfo(name = "tx_count")
    val txCount: Int,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "success")
    val success: Boolean,
    @ColumnInfo(name = "error_summary")
    val errorSummary: String?,
)
