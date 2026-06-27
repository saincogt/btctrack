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
    groupOrder: List<String> = emptyList(),
): List<AddressTreeNode> = buildNodes("", entries, balancesByAddress, groupOrder)

private fun buildNodes(
    parentPath: String,
    allEntries: List<AddressEntry>,
    balancesByAddress: Map<String, BalanceSnapshot>,
    groupOrder: List<String>,
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
        .sortedWith(compareBy(
            { (segment, _) ->
                val fullPath = if (parentPath.isEmpty()) segment else "$parentPath/$segment"
                val idx = groupOrder.indexOf(fullPath)
                if (idx < 0) Int.MAX_VALUE else idx
            },
            { (segment, _) -> segment },
        ))
        .map { (segment, _) ->
            val fullPath = if (parentPath.isEmpty()) segment else "$parentPath/$segment"
            val children = buildNodes(fullPath, allEntries, balancesByAddress, groupOrder)
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

// Returns sibling group paths in their current display order for the given group path.
fun findSiblings(nodes: List<AddressTreeNode>, targetPath: String): List<String> {
    val rootGroups = nodes.filterIsInstance<AddressTreeNode.Group>()
    if (rootGroups.any { it.path == targetPath }) return rootGroups.map { it.path }
    for (group in rootGroups) {
        val found = findSiblings(group.children, targetPath)
        if (found.isNotEmpty()) return found
    }
    return emptyList()
}

// Returns a new groupOrder list with the group moved one step in the given direction (-1 up, +1 down).
fun moveGroupInOrder(
    groupPath: String,
    siblings: List<String>,
    storedOrder: List<String>,
    direction: Int,
): List<String> {
    val idx = siblings.indexOf(groupPath)
    if (idx < 0 || siblings.size <= 1) return storedOrder
    val newIdx = (idx + direction).coerceIn(0, siblings.size - 1)
    if (newIdx == idx) return storedOrder

    val reordered = siblings.toMutableList().also {
        it.removeAt(idx)
        it.add(newIdx, groupPath)
    }

    val siblingSet = siblings.toSet()
    val result = mutableListOf<String>()
    var insertPos = -1
    for (path in storedOrder) {
        if (path in siblingSet) {
            if (insertPos < 0) insertPos = result.size
        } else {
            result.add(path)
        }
    }
    if (insertPos < 0) insertPos = result.size
    result.addAll(insertPos, reordered)
    return result
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
