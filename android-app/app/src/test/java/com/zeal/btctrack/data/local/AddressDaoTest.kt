package com.zeal.btctrack.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.zeal.btctrack.data.local.entity.AddressEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddressDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert and read addresses sorted by order then updated time`() = runBlocking {
        database.addressDao().upsert(
            AddressEntity(
                id = "b",
                address = "addr-b",
                label = "B",
                note = "",
                groupPath = "Wallet/B",
                orderValue = 2,
                watchOnly = true,
                createdAt = 200L,
                updatedAt = 200L,
            )
        )
        database.addressDao().upsert(
            AddressEntity(
                id = "a",
                address = "addr-a",
                label = "A",
                note = "",
                groupPath = "Wallet/A",
                orderValue = 1,
                watchOnly = false,
                createdAt = 100L,
                updatedAt = 100L,
            )
        )

        val items = database.addressDao().observeAll().first()

        assertEquals(listOf("addr-a", "addr-b"), items.map { it.address })
        assertEquals(1, items.first().orderValue)
    }

    @Test
    fun `find by address returns stored entity`() = runBlocking {
        database.addressDao().upsert(
            AddressEntity(
                id = "x",
                address = "bc1-test",
                label = "Test",
                note = "Note",
                groupPath = "Watching",
                orderValue = 9_999,
                watchOnly = true,
                createdAt = 1L,
                updatedAt = 2L,
            )
        )

        val entity = database.addressDao().findByAddress("bc1-test")

        assertNotNull(entity)
        assertEquals("Test", entity?.label)
        assertEquals("Watching", entity?.groupPath)
    }

    @Test
    fun `deleteByAddress removes only the targeted row`() = runBlocking {
        val keep = AddressEntity(
            id = "a", address = "addr-keep", label = "Keep", note = "",
            groupPath = "", orderValue = 1, watchOnly = true,
            createdAt = 1L, updatedAt = 1L,
        )
        val remove = AddressEntity(
            id = "b", address = "addr-remove", label = "Remove", note = "",
            groupPath = "", orderValue = 2, watchOnly = true,
            createdAt = 2L, updatedAt = 2L,
        )
        database.addressDao().upsertAll(listOf(keep, remove))
        database.addressDao().deleteByAddress("addr-remove")
        val remaining = database.addressDao().getAll()
        assertEquals(1, remaining.size)
        assertEquals("addr-keep", remaining.first().address)
    }
}
