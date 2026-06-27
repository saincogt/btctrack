package com.zeal.btctrack.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.zeal.btctrack.data.local.AppDatabase
import com.zeal.btctrack.domain.model.AddressEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddressRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: AddressRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = AddressRepositoryImpl(database.addressDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `replaceAll stores imported entries and exposes them as domain models`() = runBlocking {
        repository.replaceAll(
            listOf(
                AddressEntry(
                    id = "1",
                    address = "addr-1",
                    label = "Cold",
                    note = "Ledger",
                    groupPath = "Wallet/Personal",
                    order = 1,
                    watchOnly = true,
                    createdAt = 10L,
                    updatedAt = 10L,
                ),
                AddressEntry(
                    id = "2",
                    address = "addr-2",
                    label = "Watch",
                    note = "",
                    groupPath = "Watching",
                    order = 9_999,
                    watchOnly = false,
                    createdAt = 20L,
                    updatedAt = 20L,
                ),
            )
        )

        val items = repository.observeAll().first()

        assertEquals(2, items.size)
        assertEquals(listOf("addr-1", "addr-2"), items.map { it.address })
        assertEquals("Ledger", items.first().note)
    }

    @Test
    fun `add inserts a new entry observable via observeAll`() = runBlocking {
        val entry = AddressEntry(
            id = "new-1", address = "bc1qnew", label = "New",
            note = "test", groupPath = "Wallet", order = 5,
            watchOnly = true, createdAt = 100L, updatedAt = 100L,
        )
        repository.add(entry)
        val items = repository.observeAll().first()
        assertEquals(1, items.size)
        assertEquals("bc1qnew", items.first().address)
    }

    @Test
    fun `update changes label and note of existing entry`() = runBlocking {
        val original = AddressEntry(
            id = "upd-1", address = "bc1qupdate", label = "Old label",
            note = "old note", groupPath = "", order = 1,
            watchOnly = true, createdAt = 10L, updatedAt = 10L,
        )
        repository.add(original)
        repository.update(original.copy(label = "New label", note = "new note", updatedAt = 99L))
        val item = repository.observeAll().first().first()
        assertEquals("New label", item.label)
        assertEquals("new note", item.note)
    }

    @Test
    fun `delete removes entry by address`() = runBlocking {
        val entry = AddressEntry(
            id = "del-1", address = "bc1qdelete", label = "Del",
            note = "", groupPath = "", order = 1,
            watchOnly = true, createdAt = 10L, updatedAt = 10L,
        )
        repository.add(entry)
        repository.delete("bc1qdelete")
        val items = repository.observeAll().first()
        assertEquals(0, items.size)
    }
}
