package com.zeal.btctrack.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AddressEntry(
    val id: String,
    val address: String,
    val label: String,
    val note: String,
    val groupPath: String,
    val order: Int,
    val watchOnly: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
