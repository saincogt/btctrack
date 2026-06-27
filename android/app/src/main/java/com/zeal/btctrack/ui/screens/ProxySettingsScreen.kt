package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
fun ProxySettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val settings by container.settingsRepository.observe().collectAsStateCompat(null)
    var status by remember { mutableStateOf("") }
    var form by remember(settings) { mutableStateOf(settings?.let { SettingsFormState.from(it) } ?: SettingsFormState()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proxy settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Leave SOCKS host blank to use Orbot VPN mode (transparent routing).",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                form.socksHost,
                { form = form.copy(socksHost = it) },
                label = { Text("SOCKS host (blank = VPN mode)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                form.socksPort,
                { form = form.copy(socksPort = it) },
                label = { Text("SOCKS port") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                form.esploraBaseUrl,
                { form = form.copy(esploraBaseUrl = it) },
                label = { Text("Esplora API URL") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    scope.launch {
                        status = runCatching {
                            container.settingsRepository.update {
                                it.copy(
                                    socksHost = form.socksHost,
                                    socksPort = form.socksPort.toIntOrNull() ?: it.socksPort,
                                    esploraBaseUrl = form.esploraBaseUrl,
                                )
                            }
                            "Proxy settings saved"
                        }.getOrElse { error -> "Save failed: ${error.message}" }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }

            if (status.isNotBlank()) {
                Text(status)
            }
        }
    }
}
