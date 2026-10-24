package com.zeal.btctrack.ui

import com.zeal.btctrack.domain.model.AddressEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressSuggestionsTest {

    private fun entry(address: String, groupPath: String, label: String = "") = AddressEntry(
        id = address, address = address, label = label, note = "",
        groupPath = groupPath, order = 0, watchOnly = false,
        createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `group suggestions are empty for no entries`() {
        assertEquals(emptyList<String>(), groupSuggestions(emptyList(), ""))
    }

    @Test
    fun `group suggestions expand parent paths and dedupe`() {
        val entries = listOf(
            entry("a1", "Ledger/Main"),
            entry("a2", "Ledger/Main"),
            entry("a3", "Ledger/Savings"),
            entry("a4", "Cold"),
        )
        assertEquals(
            listOf("Cold", "Ledger", "Ledger/Main", "Ledger/Savings"),
            groupSuggestions(entries, ""),
        )
    }

    @Test
    fun `group suggestions skip blank group paths`() {
        assertEquals(listOf("Cold"), groupSuggestions(listOf(entry("a1", ""), entry("a2", "Cold")), ""))
    }

    @Test
    fun `group suggestions filter case-insensitively by substring`() {
        val entries = listOf(entry("a1", "Ledger/Main"), entry("a2", "Cold"))
        assertEquals(listOf("Ledger/Main"), groupSuggestions(entries, "mAi"))
    }

    @Test
    fun `group suggestions hide an exact match of the query`() {
        val entries = listOf(entry("a1", "Ledger/Main"))
        assertEquals(listOf("Ledger/Main"), groupSuggestions(entries, "Ledger"))
    }

    @Test
    fun `label suggestions dedupe and sort when no group given`() {
        val entries = listOf(
            entry("a1", "X", "Savings 2"),
            entry("a2", "Y", "Savings 1"),
            entry("a3", "Z", "Savings 1"),
        )
        assertEquals(listOf("Savings 1", "Savings 2"), labelSuggestions(entries, "", ""))
    }

    @Test
    fun `label suggestions put labels from current group first`() {
        val entries = listOf(
            entry("a1", "Other", "Alpha"),
            entry("a2", "Ledger/Main", "Zulu"),
            entry("a3", "Ledger/Main", "Mike"),
        )
        assertEquals(listOf("Mike", "Zulu", "Alpha"), labelSuggestions(entries, " Ledger/Main ", ""))
    }

    @Test
    fun `label suggestions do not repeat a label that exists in both partitions`() {
        val entries = listOf(
            entry("a1", "Other", "Shared"),
            entry("a2", "Ledger", "Shared"),
        )
        assertEquals(listOf("Shared"), labelSuggestions(entries, "Ledger", ""))
    }

    @Test
    fun `label suggestions filter case-insensitively and hide exact match`() {
        val entries = listOf(
            entry("a1", "", "Ledger receive 1"),
            entry("a2", "", "Ledger receive"),
            entry("a3", "", "Trezor"),
        )
        assertEquals(listOf("Ledger receive 1"), labelSuggestions(entries, "", "ledger RECEIVE"))
    }

    @Test
    fun `label suggestions skip blank labels`() {
        assertEquals(listOf("A"), labelSuggestions(listOf(entry("a1", "", ""), entry("a2", "", "A")), "", ""))
    }

    @Test
    fun `group suggestions normalize stray slashes and spaces`() {
        val entries = listOf(
            entry("a1", "Wallet/"),
            entry("a2", "Wallet//Sub"),
            entry("a3", " Wallet / Sub2 "),
        )
        assertEquals(listOf("Wallet", "Wallet/Sub", "Wallet/Sub2"), groupSuggestions(entries, ""))
    }

    @Test
    fun `group suggestions dedupe case-insensitively`() {
        val entries = listOf(entry("a1", "wallet"), entry("a2", "Wallet"))
        assertEquals(listOf("Wallet"), groupSuggestions(entries, ""))
    }

    @Test
    fun `label suggestions dedupe case-insensitively`() {
        val entries = listOf(entry("a1", "", "savings"), entry("a2", "", "Savings"))
        assertEquals(listOf("Savings"), labelSuggestions(entries, "", ""))
    }

    @Test
    fun `label suggestions match current group after normalizing it`() {
        val entries = listOf(
            entry("a1", "Other", "Alpha"),
            entry("a2", "Ledger/Main", "Zulu"),
        )
        assertEquals(listOf("Zulu", "Alpha"), labelSuggestions(entries, "Ledger / Main/", ""))
    }
}
