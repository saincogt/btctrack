package com.zeal.btctrack.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.zeal.btctrack.BtcTrackApp
import com.zeal.btctrack.domain.model.AppSettings
import com.zeal.btctrack.ui.navigation.AppNavHost
import com.zeal.btctrack.ui.theme.BtcTrackTheme

@Composable
fun BtcTrackRoot() {
    val app = LocalContext.current.applicationContext as BtcTrackApp
    val settings by app.container.settingsRepository.observe().collectAsStateCompat(AppSettings())
    val isDark = when (settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    BtcTrackTheme(darkTheme = isDark) {
        AppNavHost(app.container)
    }
}
