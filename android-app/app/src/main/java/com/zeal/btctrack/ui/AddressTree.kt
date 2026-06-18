package com.zeal.btctrack.ui

import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.model.BalanceSnapshot

sealed class AddressTreeNode {
    data class Group(
        val path: String,
        val name: String,
        val children: List<AddressTreeNode>,
        val totalSats: Long,
    ) : AddressTreeNode()

    data class Leaf(
        val entry: AddressEntry,
        val confirmedSats: Long,
    ) : AddressTreeNode()
}

data class FlatItem(val node: AddressTreeNode, val depth: Int)

fun buildAddressTree(
    entries: List<AddressEntry>,
    balancesByAddress: Map<String, BalanceSnapshot>,
): List<AddressTreeNode> = buildNodes("", entries, balancesByAddress)

private fun buildNodes(
    parentPath: String,
    allEntries: List<AddressEntry>,
    balancesByAddress: Map<String, BalanceSnapshot>,
): List<AddressTreeNode> {
    val directLeaves = allEntries
        .filter { it.groupPath == parentPath }
        .sortedWith(compareBy({ it.order }, { it.label }, { it.address }))
        .map { entry ->
            AddressTreeNode.Leaf(
                entry = entry,
                confirmedSats = balancesByAddress[entry.address]?.confirmedSats ?: 0L,
            )
        }

    val childEntries = if (parentPath.isEmpty()) {
        allEntries.filter { it.groupPath.isNotBlank() }
    } else {
        allEntries.filter { it.groupPath.startsWith("$parentPath/") }
    }

    val childGroups = childEntries
        .groupBy { entry ->
            val rest = if (parentPath.isEmpty()) entry.groupPath
                       else entry.groupPath.substring(parentPath.length + 1)
            rest.substringBefore("/")
        }
        .entries
        .sortedBy { it.key }
        .map { (segment, _) ->
            val fullPath = if (parentPath.isEmpty()) segment else "$parentPath/$segment"
            val children = buildNodes(fullPath, allEntries, balancesByAddress)
            AddressTreeNode.Group(
                path = fullPath,
                name = segment,
                children = children,
                totalSats = sumLeafSats(children),
            )
        }

    return childGroups + directLeaves
}

private fun sumLeafSats(nodes: List<AddressTreeNode>): Long = nodes.sumOf { node ->
    when (node) {
        is AddressTreeNode.Group -> node.totalSats
        is AddressTreeNode.Leaf -> node.confirmedSats
    }
}

fun flattenVisible(
    nodes: List<AddressTreeNode>,
    expandState: Map<String, Boolean>,
    depth: Int = 0,
): List<FlatItem> {
    val result = mutableListOf<FlatItem>()
    for (node in nodes) {
        result.add(FlatItem(node, depth))
        if (node is AddressTreeNode.Group && expandState[node.path] == true) {
            result.addAll(flattenVisible(node.children, expandState, depth + 1))
        }
    }
    return result
}
