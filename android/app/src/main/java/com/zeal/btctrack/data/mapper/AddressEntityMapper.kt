package com.zeal.btctrack.data.mapper

import com.zeal.btctrack.data.local.entity.AddressEntity
import com.zeal.btctrack.domain.model.AddressEntry

fun AddressEntity.toDomain(): AddressEntry = AddressEntry(
    id = id,
    address = address,
    label = label,
    note = note,
    groupPath = groupPath,
    order = orderValue,
    watchOnly = watchOnly,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun AddressEntry.toEntity(): AddressEntity = AddressEntity(
    id = id,
    address = address,
    label = label,
    note = note,
    groupPath = groupPath,
    orderValue = order,
    watchOnly = watchOnly,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
