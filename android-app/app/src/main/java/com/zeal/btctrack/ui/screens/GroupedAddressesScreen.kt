package com.zeal.btctrack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.ui.buildGroupedSections
import com.zeal.btctrack.ui.collectAsStateCompat
import com.zeal.btctrack.ui.formatBalance
import com.zeal.btctrack.ui.formatRelativeTime
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
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val biometricGate: BiometricGate? = remember(context) {
        context.findFragmentActivity()?.let(::AndroidBiometricGate)
    }

    val settings by container.settingsRepository.observe().collectAsStateCompat(null)
    val addresses by container.addressRepository.observeAll().collectAsStateCompat(emptyList())
    val balances by container.balanceRepository.observeAll().collectAsStateCompat(emptyList())
    val torReachable by container.torReachable.collectAsStateCompat(false)

    val balanceUnit = settings?.balanceUnit ?: "sats"

    val revealedAddresses = remember { mutableStateMapOf<String, Boolean>() }
    val expandedAddresses = remember { mutableStateMapOf<String, Boolean>() }
    val refreshingAddresses = remember { mutableStateMapOf<String, Boolean>() }
    var menuAddress by remember { mutableStateOf<String?>(null) }

    val balancesByAddress = remember(balances) { balances.associateBy { it.address } }
    val sections = remember(addresses, balances) {
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
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No addresses yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap + to add your first address",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                sections.forEach { section ->
                    item(key = "header_${section.title}") {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(section.items, key = { it.address }) { item ->
                        val expanded = expandedAddresses[item.address] == true
                        val revealed = revealedAddresses[item.address] == true
                        val isRefreshing = refreshingAddresses[item.address] == true
                        val snapshot = balancesByAddress[item.address]
                        val totalSats = (snapshot?.confirmedSats ?: 0L) + (snapshot?.unconfirmedSats ?: 0L)

                        Box {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .combinedClickable(
                                        onClick = {
                                            expandedAddresses[item.address] = !expanded
                                        },
                                        onLongClick = { menuAddress = item.address },
                                    ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    // Collapsed row: label + balance + refresh button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            item.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            formatBalance(totalSats, balanceUnit),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        if (isRefreshing) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    if (!torReachable) {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Tor not reachable")
                                                        }
                                                        return@IconButton
                                                    }
                                                    scope.launch {
                                                        refreshingAddresses[item.address] = true
                                                        try {
                                                            container.refreshAddress(item.address)
                                                        } finally {
                                                            refreshingAddresses[item.address] = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp),
                                                enabled = torReachable,
                                            ) {
                                                Icon(
                                                    Icons.Default.Refresh,
                                                    contentDescription = "Refresh",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = if (torReachable) MaterialTheme.colorScheme.primary
                                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                )
                                            }
                                        }
                                    }

                                    // Expanded section
                                    AnimatedVisibility(visible = expanded) {
                                        Column(
                                            modifier = Modifier.padding(top = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            // Address row with copy
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onClick = {
                                                            clipboard.setText(AnnotatedString(item.address))
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar("Address copied")
                                                            }
                                                        },
                                                        onLongClick = {},
                                                    )
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    if (revealed) item.address else item.address.redactedAddress(),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                Text(
                                                    "Copy",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(start = 8.dp),
                                                )
                                            }
                                            DetailRow("Confirmed", formatBalance(snapshot?.confirmedSats ?: 0L, balanceUnit))
                                            DetailRow("Unconfirmed", formatBalance(snapshot?.unconfirmedSats ?: 0L, balanceUnit))
                                            DetailRow("Transactions", "${snapshot?.txCount ?: 0}")
                                            DetailRow("Updated", formatRelativeTime(snapshot?.fetchedAt ?: 0L))
                                        }
                                    }
                                }
                            }

                            // Dropdown menu
                            DropdownMenu(
                                expanded = menuAddress == item.address,
                                onDismissRequest = { menuAddress = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (revealed) "Hide address" else "Reveal address") },
                                    onClick = {
                                        menuAddress = null
                                        scope.launch {
                                            val requireBiometric = settings?.requireBiometricForDetails ?: true
                                            val gate = biometricGate ?: object : BiometricGate {
                                                override suspend fun authenticate(request: BiometricPromptRequest): BiometricGateResult =
                                                    BiometricGateResult(false, "Biometric unavailable")
                                            }
                                            val result = SensitiveRevealController(gate).toggle(
                                                currentlyVisible = revealed,
                                                requireBiometric = requireBiometric,
                                                request = BiometricPromptRequest(
                                                    title = "Reveal full address",
                                                    subtitle = item.label,
                                                    description = "Authenticate to reveal tracked Bitcoin address",
                                                ),
                                            )
                                            revealedAddresses[item.address] = result.visible
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
                                        val snapshot2: AddressEntry? = addresses.find { it.address == item.address }
                                        if (snapshot2 != null) {
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Deleted ${snapshot2.label}",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Long,
                                                )
                                                if (result != SnackbarResult.ActionPerformed) {
                                                    container.addressRepository.delete(snapshot2.address)
                                                } else {
                                                    scope.launch { snackbarHostState.showSnackbar("Undo successful") }
                                                }
                                            }
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar("Address not found") }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.size(80.dp)) }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
