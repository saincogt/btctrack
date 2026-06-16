package com.zeal.btctrack.ui

import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.model.BalanceSnapshot

fun String.redactedAddress(): String =
    if (length <= 12) this else "${take(6)}...${takeLast(4)}"

data class GroupedAddressSection(
    val title: String,
    val items: List<GroupedAddressRow>,
)

data class GroupedAddressRow(
    val address: String,
    val label: String,
    val groupPath: String,
    val order: Int,
    val confirmedSats: Long,
    val txCount: Int,
)

fun buildGroupedSections(
    addresses: List<AddressEntry>,
    balancesByAddress: Map<String, BalanceSnapshot>,
): List<GroupedAddressSection> {
    val sorted = addresses.sortedWith(compareBy<AddressEntry> { it.groupPath }.thenBy { it.order }.thenBy { it.label }.thenBy { it.address })
    return sorted.groupBy { it.groupPath.ifBlank { "No group" } }
        .map { (group, entries) ->
            GroupedAddressSection(
                title = group,
                items = entries.map { entry ->
                    val snapshot = balancesByAddress[entry.address]
                    GroupedAddressRow(
                        address = entry.address,
                        label = entry.label.ifBlank { entry.address.redactedAddress() },
                        groupPath = entry.groupPath,
                        order = entry.order,
                        confirmedSats = snapshot?.confirmedSats ?: 0L,
                        txCount = snapshot?.txCount ?: 0,
                    )
                }
            )
        }
}
