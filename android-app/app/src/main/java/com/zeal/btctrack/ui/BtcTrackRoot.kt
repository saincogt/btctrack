package com.zeal.btctrack.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.zeal.btctrack.BtcTrackApp
import com.zeal.btctrack.ui.navigation.AppNavHost
import com.zeal.btctrack.ui.theme.BtcTrackTheme

@Composable
fun BtcTrackRoot() {
    val app = LocalContext.current.applicationContext as BtcTrackApp
    BtcTrackTheme {
        MaterialTheme {
            AppNavHost(app.container)
        }
    }
}
