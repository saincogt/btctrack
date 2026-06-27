package com.zeal.btctrack.data.mapper.dto

import kotlinx.serialization.Serializable

@Serializable
data class CliAddressDto(
    val address: String,
    val label: String? = null,
    val note: String? = null,
)
