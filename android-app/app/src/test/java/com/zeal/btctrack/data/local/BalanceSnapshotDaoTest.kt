package com.zeal.btctrack.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.zeal.btctrack.data.local.entity.BalanceSnapshotEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BalanceSnapshotDaoTest {
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
    fun `replace snapshots and observe all`() = runBlocking {
        database.balanceSnapshotDao().upsert(
            BalanceSnapshotEntity(
                address = "addr-1",
                confirmedSats = 100,
                unconfirmedSats = 5,
                txCount = 2,
                fetchedAt = 10L,
                source = "onion",
                success = true,
                errorSummary = null,
            )
        )

        var snapshots = database.balanceSnapshotDao().observeAll().first()
        assertEquals(1, snapshots.size)
        assertEquals(100, snapshots.first().confirmedSats)

        database.balanceSnapshotDao().upsert(
            BalanceSnapshotEntity(
                address = "addr-1",
                confirmedSats = 250,
                unconfirmedSats = 0,
                txCount = 3,
                fetchedAt = 20L,
                source = "onion",
                success = true,
                errorSummary = null,
            )
        )

        snapshots = database.balanceSnapshotDao().observeAll().first()
        assertEquals(1, snapshots.size)
        assertEquals(250, snapshots.first().confirmedSats)
        assertEquals(20L, snapshots.first().fetchedAt)
    }
}
