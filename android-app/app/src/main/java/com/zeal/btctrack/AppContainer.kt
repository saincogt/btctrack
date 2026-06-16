package com.zeal.btctrack

import android.content.Context
import androidx.room.Room
import com.zeal.btctrack.background.BackgroundRefreshScheduler
import com.zeal.btctrack.data.importer.AddressImportService
import com.zeal.btctrack.data.local.AppDatabase
import com.zeal.btctrack.data.local.SettingsStore
import com.zeal.btctrack.data.remote.TorHealthChecker
import com.zeal.btctrack.data.remote.TorOnlyEsploraClient

import com.zeal.btctrack.data.repository.AddressRepositoryImpl
import com.zeal.btctrack.data.repository.BalanceRepositoryImpl
import com.zeal.btctrack.data.repository.SettingsRepositoryImpl
import com.zeal.btctrack.domain.model.BalanceSnapshot
import com.zeal.btctrack.domain.repository.AddressRepository
import com.zeal.btctrack.domain.repository.BalanceRepository
import com.zeal.btctrack.domain.repository.SettingsRepository
import com.zeal.btctrack.domain.usecase.ExportAddressesUseCase
import com.zeal.btctrack.domain.usecase.ImportAddressesUseCase
import com.zeal.btctrack.domain.usecase.RefreshTrackedAddressesUseCase
import kotlinx.coroutines.flow.first

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val backgroundRefreshScheduler = BackgroundRefreshScheduler(appContext)

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "btctrack.db",
    ).build()

    private val settingsStore = SettingsStore(appContext)
    val addressRepository: AddressRepository = AddressRepositoryImpl(database.addressDao())
    val balanceRepository: BalanceRepository = BalanceRepositoryImpl(database.balanceSnapshotDao())
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(settingsStore)

    private val importService = AddressImportService()
    val importAddressesUseCase = ImportAddressesUseCase(importService, addressRepository)
    val exportAddressesUseCase = ExportAddressesUseCase(addressRepository)

    suspend fun refreshAddress(address: String): BalanceSnapshot {
        val settings = settingsRepository.observe().first()
        val client = TorOnlyEsploraClient(
            baseUrl = settings.esploraBaseUrl,
            appSettings = settings,
        )
        val snapshot = client.fetchBalanceSnapshot(address)
        balanceRepository.upsertAll(listOf(snapshot))
        return snapshot
    }

    suspend fun refreshAllTrackedAddresses() = RefreshTrackedAddressesUseCase(
        addressRepository = addressRepository,
        settingsRepository = settingsRepository,
        refreshAddressBalance = ::refreshAddress,
        balanceRepository = balanceRepository,
    ).invoke()

    suspend fun torHealthStatus(): String {
        val settings = settingsRepository.observe().first()
        val status = TorHealthChecker(baseUrl = settings.esploraBaseUrl, appSettings = settings).check()
        return if (status.ok) {
            "Tor reachable via ${settings.socksHost}:${settings.socksPort}"
        } else {
            "Tor unavailable: ${status.message}"
        }
    }

    suspend fun syncBackgroundRefreshSchedule() {
        val settings = settingsRepository.observe().first()
        backgroundRefreshScheduler.sync(settings)
    }
}
