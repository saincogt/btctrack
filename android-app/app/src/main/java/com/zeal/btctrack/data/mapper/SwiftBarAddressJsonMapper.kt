package com.zeal.btctrack.data.mapper

import com.zeal.btctrack.data.mapper.dto.SwiftBarAddressDto
import com.zeal.btctrack.domain.model.AddressEntry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
class SwiftBarAddressJsonMapper(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) {
    fun parse(inputStream: InputStream, nowEpochMillis: Long): List<AddressEntry> {
        val items = json.decodeFromStream<List<SwiftBarAddressDto>>(inputStream)
        return items.map { dto ->
            AddressEntry(
                id = UUID.randomUUID().toString(),
                address = dto.address.trim(),
                label = dto.label.orEmpty(),
                note = "",
                groupPath = dto.groupPath.orEmpty(),
                order = dto.order ?: DEFAULT_ORDER,
                watchOnly = dto.watchOnly ?: false,
                createdAt = nowEpochMillis,
                updatedAt = nowEpochMillis,
            )
        }
    }

    companion object {
        const val DEFAULT_ORDER: Int = 9_999
    }
}
