package com.zeal.btctrack.data.repository

import com.zeal.btctrack.data.local.SettingsStore
import com.zeal.btctrack.domain.model.AppSettings
import com.zeal.btctrack.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val settingsStore: SettingsStore,
) : SettingsRepository {
    override fun observe(): Flow<AppSettings> = settingsStore.settings

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settingsStore.update(transform)
    }
}
