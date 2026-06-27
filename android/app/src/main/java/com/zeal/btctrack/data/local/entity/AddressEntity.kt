package com.zeal.btctrack.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "address")
    val address: String,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "note")
    val note: String,
    @ColumnInfo(name = "group_path")
    val groupPath: String,
    @ColumnInfo(name = "order_value")
    val orderValue: Int,
    @ColumnInfo(name = "watch_only")
    val watchOnly: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
