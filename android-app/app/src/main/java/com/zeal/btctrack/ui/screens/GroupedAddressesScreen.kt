package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.ui.buildGroupedSections
import com.zeal.btctrack.ui.collectAsStateCompat
import com.zeal.btctrack.ui.redactedAddress
import com.zeal.btctrack.ui.security.AndroidBiometricGate
import com.zeal.btctrack.ui.security.BiometricGate
import com.zeal.btctrack.ui.security.BiometricGateResult
import com.zeal.btctrack.ui.security.BiometricPromptRequest
import com.zeal.btctrack.ui.security.SensitiveRevealController
import com.zeal.btctrack.ui.security.findFragmentActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupedAddressesScreen(
    container: AppContainer,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val biometricGate: BiometricGate? = remember(context) {
        context.findFragmentActivity()?.let(::AndroidBiometricGate)
    }
    val settings by container.settingsRepository.observe().collectAsStateCompat(null)
    val addresses by container.addressRepository.observeAll().collectAsStateCompat(emptyList())
    val balances by container.balanceRepository.observeAll().collectAsStateCompat(emptyList())
    val revealedAddresses = remember { mutableStateMapOf<String, Boolean>() }
    val revealStatuses = remember { mutableStateMapOf<String, String>() }
    var menuAddress by remember { mutableStateOf<String?>(null) }

    val sections = remember(addresses, balances) {
        val balancesByAddress = balances.associateBy { it.address }
        buildGroupedSections(addresses, balancesByAddress)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Addresses") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add address")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (sections.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("No tracked addresses yet.", style = MaterialTheme.typography.titleLarge)
                Text("Tap + to add your first address.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { section ->
                    item {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(section.items, key = { it.address }) { item ->
                        val visible = revealedAddresses[item.address] == true
                        Box {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { menuAddress = item.address },
                                    ),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(item.label, style = MaterialTheme.typography.titleMedium)
                                    Text(if (visible) item.address else item.address.redactedAddress())
                                    Text("Confirmed: ${item.confirmedSats} sats")
                                    Text("Tx count: ${item.txCount}")
                                    Text(revealStatuses[item.address] ?: "Address hidden")
                                }
                            }
                            DropdownMenu(
                                expanded = menuAddress == item.address,
                                onDismissRequest = { menuAddress = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (visible) "Hide address" else "Reveal address") },
                                    onClick = {
                                        menuAddress = null
                                        scope.launch {
                                            val requireBiometric = settings?.requireBiometricForDetails ?: true
                                            val gate = biometricGate ?: object : BiometricGate {
                                                override suspend fun authenticate(request: BiometricPromptRequest): BiometricGateResult =
                                                    BiometricGateResult(false, "Biometric unavailable")
                                            }
                                            val result = SensitiveRevealController(gate).toggle(
                                                currentlyVisible = visible,
                                                requireBiometric = requireBiometric,
                                                request = BiometricPromptRequest(
                                                    title = "Reveal full address",
                                                    subtitle = item.label,
                                                    description = "Authenticate to reveal tracked Bitcoin address",
                                                ),
                                            )
                                            revealedAddresses[item.address] = result.visible
                                            revealStatuses[item.address] = result.message
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        menuAddress = null
                                        onEditClick(item.address)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        menuAddress = null
                                        val snapshot: AddressEntry? = addresses.find { it.address == item.address }
                                        if (snapshot != null) {
                                            scope.launch {
                                                container.addressRepository.delete(snapshot.address)
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Deleted ${snapshot.label}",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short,
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    container.addressRepository.add(snapshot)
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
