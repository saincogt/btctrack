package com.zeal.btctrack.data.mapper.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SwiftBarAddressDto(
    val address: String,
    val label: String? = null,
    @SerialName("group") val groupPath: String? = null,
    val order: Int? = null,
    @SerialName("watch_only") val watchOnly: Boolean? = null,
)
