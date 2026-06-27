package com.zeal.btctrack.domain.repository

import com.zeal.btctrack.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
