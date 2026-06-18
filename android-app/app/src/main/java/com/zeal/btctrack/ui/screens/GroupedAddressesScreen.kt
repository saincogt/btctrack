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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.ui.buildGroupedSections
import com.zeal.btctrack.ui.collectAsStateCompat
import com.zeal.btctrack.ui.formatBalance
import com.zeal.btctrack.ui.formatRelativeTime
import com.zeal.btctrack.ui.redactedAddress
import androidx.biometric.BiometricManager
import com.zeal.btctrack.ui.security.AndroidBiometricGate
import com.zeal.btctrack.ui.security.BiometricGate
import com.zeal.btctrack.ui.security.BiometricPromptRequest
import com.zeal.btctrack.ui.security.SensitiveRevealController
import com.zeal.btctrack.ui.security.findFragmentActivity
import com.zeal.btctrack.ui.theme.BitcoinOrange
import com.zeal.btctrack.ui.theme.MonoBodyStyle
import com.zeal.btctrack.ui.theme.SectionLabelStyle
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
    val biometricAvailable = remember(context) {
        val activity = context.findFragmentActivity() ?: return@remember false
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
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
    var detailsAddress by remember { mutableStateOf<String?>(null) }

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
                        val groupExcluded = settings?.excludedGroups?.contains(section.title) == true
                        val groupTotal = section.items.sumOf { it.confirmedSats }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    section.title.uppercase(),
                                    style = SectionLabelStyle,
                                    color = if (groupExcluded)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        BitcoinOrange,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    formatBalance(groupTotal, balanceUnit),
                                    style = MonoBodyStyle,
                                    color = if (groupExcluded)
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val current = settings?.excludedGroups ?: emptySet()
                                            val updated = if (groupExcluded) current - section.title else current + section.title
                                            container.settingsRepository.update { it.copy(excludedGroups = updated) }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        if (groupExcluded) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (groupExcluded) "Include in total" else "Exclude from total",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (groupExcluded)
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    items(section.items, key = { it.address }) { item ->
                        val expanded = expandedAddresses[item.address] == true
                        val revealed = revealedAddresses[item.address] == true
                        val isRefreshing = refreshingAddresses[item.address] == true
                        val snapshot = balancesByAddress[item.address]
                        val totalSats = snapshot?.confirmedSats ?: 0L

                        val hasLabel = item.label.isNotBlank()
                        val truncatedAddress = "${item.address.take(6)}...${item.address.takeLast(4)}"

                        Box {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { expandedAddresses[item.address] = !expanded },
                                        onLongClick = { menuAddress = item.address },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            if (hasLabel) item.label else truncatedAddress,
                                            style = if (hasLabel) MaterialTheme.typography.bodyMedium else MonoBodyStyle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (hasLabel) {
                                            Text(
                                                truncatedAddress,
                                                style = MonoBodyStyle,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        formatBalance(totalSats, balanceUnit),
                                        style = MonoBodyStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    if (isRefreshing) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        IconButton(
                                            onClick = {
                                                if (!torReachable) {
                                                    scope.launch { snackbarHostState.showSnackbar("Tor not reachable") }
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

                                AnimatedVisibility(visible = expanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(
                                                    onClick = {
                                                        clipboard.setText(AnnotatedString(item.address))
                                                        scope.launch { snackbarHostState.showSnackbar("Address copied") }
                                                    },
                                                    onLongClick = {},
                                                )
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                if (revealed) item.address else item.address.redactedAddress(),
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
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
                                        DetailRow("Balance", formatBalance(snapshot?.confirmedSats ?: 0L, balanceUnit))
                                        DetailRow("Updated", formatRelativeTime(snapshot?.fetchedAt ?: 0L))
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = menuAddress == item.address,
                                onDismissRequest = { menuAddress = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (revealed) "Hide address" else "Reveal address") },
                                    onClick = {
                                        menuAddress = null
                                        scope.launch {
                                            val requireBiometric = biometricAvailable &&
                                                (settings?.requireBiometricForReveal ?: true)
                                            val gate = biometricGate ?: SensitiveRevealController.noOpGate()
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
                                    text = { Text("Details") },
                                    onClick = {
                                        menuAddress = null
                                        detailsAddress = item.address
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
                item { Spacer(Modifier.size(80.dp)) }
            }
        }

        detailsAddress?.let { addr ->
            val entry = addresses.find { it.address == addr }
            val snap = balancesByAddress[addr]
            AlertDialog(
                onDismissRequest = { detailsAddress = null },
                confirmButton = {
                    TextButton(onClick = { detailsAddress = null }) { Text("Close") }
                },
                title = { Text(entry?.label?.ifBlank { addr.redactedAddress() } ?: addr.redactedAddress()) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DetailRow("Address", addr)
                        if (!entry?.groupPath.isNullOrBlank()) {
                            DetailRow("Group", entry!!.groupPath)
                        }
                        if (!entry?.note.isNullOrBlank()) {
                            DetailRow("Note", entry!!.note)
                        }
                        DetailRow("Confirmed", formatBalance(snap?.confirmedSats ?: 0L, balanceUnit))
                        DetailRow("Unconfirmed", formatBalance(snap?.unconfirmedSats ?: 0L, balanceUnit))
                        DetailRow("Transactions", "${snap?.txCount ?: 0}")
                        DetailRow("Updated", formatRelativeTime(snap?.fetchedAt ?: 0L))
                        if (snap?.success == false && !snap.errorSummary.isNullOrBlank()) {
                            DetailRow("Error", snap.errorSummary)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
