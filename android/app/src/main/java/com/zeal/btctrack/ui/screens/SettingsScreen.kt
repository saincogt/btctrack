package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.ui.collectAsStateCompat
import com.zeal.btctrack.ui.theme.SectionLabelStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onProxySettingsClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val settings by container.settingsRepository.observe().collectAsStateCompat(null)
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("GENERAL")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ToggleRow(
                    label = "Show balance by default",
                    checked = settings?.showTotalBalance ?: true,
                    onCheckedChange = { checked ->
                        scope.launch {
                            container.settingsRepository.update { it.copy(showTotalBalance = checked) }
                        }
                    },
                )
                ToggleRow(
                    label = "Tor required",
                    checked = settings?.torRequired ?: true,
                    onCheckedChange = { checked ->
                        scope.launch {
                            container.settingsRepository.update { it.copy(torRequired = checked) }
                        }
                    },
                )
            }

            HorizontalDivider()
            SectionHeader("DISPLAY")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("light" to "Light", "system" to "System", "dark" to "Dark").forEach { (value, label) ->
                        FilterChip(
                            selected = (settings?.themeMode ?: "system") == value,
                            onClick = {
                                scope.launch {
                                    container.settingsRepository.update { it.copy(themeMode = value) }
                                }
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Balance unit", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("sats", "BTC").forEach { unit ->
                        FilterChip(
                            selected = (settings?.balanceUnit ?: "sats") == unit,
                            onClick = {
                                scope.launch {
                                    container.settingsRepository.update { it.copy(balanceUnit = unit) }
                                }
                            },
                            label = { Text(unit) },
                        )
                    }
                }
            }

            HorizontalDivider()
            SectionHeader("NETWORK")
            ListItem(
                headlineContent = { Text("Proxy settings") },
                supportingContent = { Text("Tor SOCKS proxy and Esplora URL") },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onProxySettingsClick),
            )

            HorizontalDivider()
            SectionHeader("DATA")
            ListItem(
                headlineContent = { Text("Import addresses") },
                modifier = Modifier.clickable(onClick = onImportClick),
            )
            ListItem(
                headlineContent = { Text("Export addresses") },
                modifier = Modifier.clickable(onClick = onExportClick),
            )
            ListItem(
                headlineContent = {
                    Text("Clear balance cache", color = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.clickable {
                    scope.launch {
                        container.balanceRepository.clearAll()
                        snackbarHostState.showSnackbar("Balance cache cleared")
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = SectionLabelStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
