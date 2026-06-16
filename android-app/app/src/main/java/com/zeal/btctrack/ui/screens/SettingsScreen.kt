package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.ui.SettingsFormState
import com.zeal.btctrack.ui.collectAsStateCompat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val settings by container.settingsRepository.observe().collectAsStateCompat(null)
    var status by remember { mutableStateOf("Settings are stored locally only.") }
    var form by remember(settings) { mutableStateOf(settings?.let { SettingsFormState.from(it) } ?: SettingsFormState()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(form.socksHost, { form = form.copy(socksHost = it) }, label = { Text("SOCKS host") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.socksPort, { form = form.copy(socksPort = it) }, label = { Text("SOCKS port") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.refreshIntervalMinutes, { form = form.copy(refreshIntervalMinutes = it) }, label = { Text("Refresh interval minutes") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.jitterMinMs, { form = form.copy(jitterMinMs = it) }, label = { Text("Jitter min ms") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.jitterMaxMs, { form = form.copy(jitterMaxMs = it) }, label = { Text("Jitter max ms") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.esploraBaseUrl, { form = form.copy(esploraBaseUrl = it) }, label = { Text("Esplora API URL") }, modifier = Modifier.fillMaxWidth())
            ToggleRow("Tor required", form.torRequired) { form = form.copy(torRequired = it) }
            ToggleRow("Show total balance", form.showTotalBalance) { form = form.copy(showTotalBalance = it) }
            ToggleRow("Biometric for details", form.requireBiometricForDetails) { form = form.copy(requireBiometricForDetails = it) }
            ToggleRow("Biometric for reveal", form.requireBiometricForReveal) { form = form.copy(requireBiometricForReveal = it) }

            Button(
                onClick = {
                    scope.launch {
                        status = runCatching {
                            container.settingsRepository.update {
                                it.copy(
                                    torRequired = form.torRequired,
                                    socksHost = form.socksHost,
                                    socksPort = form.socksPort.toInt(),
                                    refreshIntervalMinutes = form.refreshIntervalMinutes.toInt(),
                                    jitterMinMs = form.jitterMinMs.toLong(),
                                    jitterMaxMs = form.jitterMaxMs.toLong(),
                                    showTotalBalance = form.showTotalBalance,
                                    requireBiometricForDetails = form.requireBiometricForDetails,
                                    requireBiometricForReveal = form.requireBiometricForReveal,
                                    esploraBaseUrl = form.esploraBaseUrl,
                                )
                            }
                            container.syncBackgroundRefreshSchedule()
                            "Settings saved"
                        }.getOrElse { error -> "Save failed: ${error.message}" }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save settings") }

            Button(
                onClick = {
                    scope.launch {
                        container.balanceRepository.clearAll()
                        status = "Cached balances cleared"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear balance cache") }

            Text(status)
            Button(onClick = onImportClick, modifier = Modifier.fillMaxWidth()) {
                Text("Import addresses")
            }
            Button(onClick = onExportClick, modifier = Modifier.fillMaxWidth()) {
                Text("Export addresses")
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
