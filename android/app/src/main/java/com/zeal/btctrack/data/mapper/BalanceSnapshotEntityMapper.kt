package com.zeal.btctrack.data.mapper

import com.zeal.btctrack.data.local.entity.BalanceSnapshotEntity
import com.zeal.btctrack.domain.model.BalanceSnapshot

fun BalanceSnapshotEntity.toDomain(): BalanceSnapshot = BalanceSnapshot(
    address = address,
    confirmedSats = confirmedSats,
    unconfirmedSats = unconfirmedSats,
    txCount = txCount,
    fetchedAt = fetchedAt,
    source = source,
    success = success,
    errorSummary = errorSummary,
)

fun BalanceSnapshot.toEntity(): BalanceSnapshotEntity = BalanceSnapshotEntity(
    address = address,
    confirmedSats = confirmedSats,
    unconfirmedSats = unconfirmedSats,
    txCount = txCount,
    fetchedAt = fetchedAt,
    source = source,
    success = success,
    errorSummary = errorSummary,
)
