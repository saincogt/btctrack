package com.zeal.btctrack.ui

import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.model.BalanceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressTreeTest {

    private fun entry(address: String, groupPath: String, label: String = "", order: Int = 0) = AddressEntry(
        id = address, address = address, label = label, note = "",
        groupPath = groupPath, order = order, watchOnly = false,
        createdAt = 0L, updatedAt = 0L,
    )

    private fun snap(address: String, sats: Long) = BalanceSnapshot(
        address = address, confirmedSats = sats, unconfirmedSats = 0L,
        txCount = 0, fetchedAt = 0L, source = "test",
        success = true, errorSummary = null,
    )

    @Test
    fun `blank groupPath becomes root leaf`() {
        val tree = buildAddressTree(listOf(entry("addr1", "")), emptyMap())
        assertEquals(1, tree.size)
        assert(tree[0] is AddressTreeNode.Leaf)
    }

    @Test
    fun `single-segment groupPath becomes root group containing a leaf`() {
        val tree = buildAddressTree(listOf(entry("addr1", "Cold")), emptyMap())
        assertEquals(1, tree.size)
        val group = tree[0] as AddressTreeNode.Group
        assertEquals("Cold", group.path)
        assertEquals("Cold", group.name)
        assertEquals(1, group.children.size)
        assert(group.children[0] is AddressTreeNode.Leaf)
    }

    @Test
    fun `two-segment groupPath builds two-level tree`() {
        val tree = buildAddressTree(listOf(entry("addr1", "Cold/Alex")), emptyMap())
        assertEquals(1, tree.size)
        val root = tree[0] as AddressTreeNode.Group
        assertEquals("Cold", root.path)
        assertEquals("Cold", root.name)
        val child = root.children[0] as AddressTreeNode.Group
        assertEquals("Cold/Alex", child.path)
        assertEquals("Alex", child.name)
        assert(child.children[0] is AddressTreeNode.Leaf)
    }

    @Test
    fun `totalSats aggregates all descendant leaf sats`() {
        val entries = listOf(entry("addr1", "Cold/Alex"), entry("addr2", "Cold/Bob"))
        val balances = mapOf("addr1" to snap("addr1", 100L), "addr2" to snap("addr2", 50L))
        val tree = buildAddressTree(entries, balances)
        val root = tree[0] as AddressTreeNode.Group
        assertEquals(150L, root.totalSats)
        val alex = root.children[0] as AddressTreeNode.Group
        val bob = root.children[1] as AddressTreeNode.Group
        assertEquals(100L, alex.totalSats)
        assertEquals(50L, bob.totalSats)
    }

    @Test
    fun `entries with same parent group are merged`() {
        val entries = listOf(entry("addr1", "Cold"), entry("addr2", "Cold"))
        val tree = buildAddressTree(entries, emptyMap())
        assertEquals(1, tree.size)
        val group = tree[0] as AddressTreeNode.Group
        assertEquals(2, group.children.size)
    }

    @Test
    fun `groups appear before leaves at same level`() {
        val entries = listOf(
            entry("addr1", "Cold"),
            entry("addr2", "Cold/Alex"),
            entry("addr3", ""),
        )
        val tree = buildAddressTree(entries, emptyMap())
        assert(tree[0] is AddressTreeNode.Group)
        assert(tree[1] is AddressTreeNode.Leaf)
        val coldGroup = tree[0] as AddressTreeNode.Group
        assert(coldGroup.children[0] is AddressTreeNode.Group)
        assert(coldGroup.children[1] is AddressTreeNode.Leaf)
    }

    @Test
    fun `flattenVisible returns only root items when all collapsed`() {
        val entries = listOf(entry("addr1", "Cold/Alex"))
        val tree = buildAddressTree(entries, emptyMap())
        val flat = flattenVisible(tree, emptyMap())
        assertEquals(1, flat.size)
        assertEquals(0, flat[0].depth)
        assert(flat[0].node is AddressTreeNode.Group)
    }

    @Test
    fun `flattenVisible includes children when group is expanded`() {
        val entries = listOf(entry("addr1", "Cold"))
        val tree = buildAddressTree(entries, emptyMap())
        val flat = flattenVisible(tree, mapOf("Cold" to true))
        assertEquals(2, flat.size)
        assertEquals(0, flat[0].depth)
        assertEquals(1, flat[1].depth)
        assert(flat[0].node is AddressTreeNode.Group)
        assert(flat[1].node is AddressTreeNode.Leaf)
    }

    @Test
    fun `flattenVisible increments depth for each nesting level`() {
        val entries = listOf(entry("addr1", "Cold/Alex"))
        val tree = buildAddressTree(entries, emptyMap())
        val flat = flattenVisible(tree, mapOf("Cold" to true, "Cold/Alex" to true))
        assertEquals(3, flat.size)
        assertEquals(0, flat[0].depth)
        assertEquals(1, flat[1].depth)
        assertEquals(2, flat[2].depth)
    }

    @Test
    fun `collapsed group does not show children even if nested groups are expanded`() {
        val entries = listOf(entry("addr1", "A/B/C"))
        val tree = buildAddressTree(entries, emptyMap())
        val flat = flattenVisible(tree, mapOf("A/B" to true, "A/B/C" to true))
        assertEquals(1, flat.size)
    }
}
