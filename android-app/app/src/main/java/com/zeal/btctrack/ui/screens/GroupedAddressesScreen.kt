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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.ui.AddressTreeNode
import com.zeal.btctrack.ui.buildAddressTree
import com.zeal.btctrack.ui.collectAsStateCompat
import com.zeal.btctrack.ui.flattenVisible
import com.zeal.btctrack.ui.formatBalance
import com.zeal.btctrack.ui.formatRelativeTime
import com.zeal.btctrack.ui.redactedAddress
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
    val groupExpandState = remember { mutableStateMapOf<String, Boolean>() }
    val refreshingAddresses = remember { mutableStateMapOf<String, Boolean>() }
    var menuAddress by remember { mutableStateOf<String?>(null) }
    var detailsAddress by remember { mutableStateOf<String?>(null) }

    val balancesByAddress = remember(balances) { balances.associateBy { it.address } }
    val tree = remember(addresses, balances) {
        buildAddressTree(addresses, balancesByAddress)
    }
    val flatItems = flattenVisible(tree, groupExpandState)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Addresses") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add address")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (tree.isEmpty()) {
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
                items(
                    flatItems,
                    key = { item ->
                        when (val node = item.node) {
                            is AddressTreeNode.Group -> "g:${node.path}"
                            is AddressTreeNode.Leaf -> "l:${node.entry.address}"
                        }
                    },
                ) { flatItem ->
                    when (val node = flatItem.node) {
                        is AddressTreeNode.Group -> {
                            val expanded = groupExpandState[node.path] == true
                            val excluded = settings?.excludedGroups?.contains(node.path) == true
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { groupExpandState[node.path] = !expanded },
                                            onLongClick = {},
                                        )
                                        .padding(
                                            start = (16 + flatItem.depth * 16).dp,
                                            end = 4.dp,
                                            top = 12.dp,
                                            bottom = 4.dp,
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        if (expanded) Icons.Default.KeyboardArrowDown
                                        else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = if (expanded) "Collapse" else "Expand",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        node.name.uppercase(),
                                        style = SectionLabelStyle,
                                        color = if (excluded) MaterialTheme.colorScheme.onSurfaceVariant else BitcoinOrange,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        formatBalance(node.totalSats, balanceUnit),
                                        style = MonoBodyStyle,
                                        color = if (excluded)
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val current = settings?.excludedGroups ?: emptySet()
                                                val updated = if (excluded) current - node.path else current + node.path
                                                container.settingsRepository.update { it.copy(excludedGroups = updated) }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            if (excluded) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (excluded) "Include in total" else "Exclude from total",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (excluded)
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        is AddressTreeNode.Leaf -> {
                            val entry = node.entry
                            val expanded = expandedAddresses[entry.address] == true
                            val revealed = revealedAddresses[entry.address] == true
                            val isRefreshing = refreshingAddresses[entry.address] == true
                            val snapshot = balancesByAddress[entry.address]
                            val hasLabel = entry.label.isNotBlank()
                            val truncatedAddress = "${entry.address.take(6)}...${entry.address.takeLast(4)}"
                            val startPad = (16 + flatItem.depth * 16).dp

                            Box {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { expandedAddresses[entry.address] = !expanded },
                                            onLongClick = { menuAddress = entry.address },
                                        )
                                        .padding(start = startPad, end = 16.dp, top = 12.dp, bottom = 12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                if (hasLabel) entry.label else truncatedAddress,
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
                                            formatBalance(node.confirmedSats, balanceUnit),
                                            style = MonoBodyStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        if (isRefreshing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    if (!torReachable) {
                                                        scope.launch { snackbarHostState.showSnackbar("Tor not reachable") }
                                                        return@IconButton
                                                    }
                                                    scope.launch {
                                                        refreshingAddresses[entry.address] = true
                                                        try {
                                                            container.refreshAddress(entry.address)
                                                        } finally {
                                                            refreshingAddresses[entry.address] = false
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
                                                            clipboard.setText(AnnotatedString(entry.address))
                                                            scope.launch { snackbarHostState.showSnackbar("Address copied") }
                                                        },
                                                        onLongClick = {},
                                                    )
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    if (revealed) entry.address else entry.address.redactedAddress(),
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
                                            DetailRow("Balance", formatBalance(snapshot?.confirmedSats ?: 0L, balanceUnit))
                                            DetailRow("Updated", formatRelativeTime(snapshot?.fetchedAt ?: 0L))
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = menuAddress == entry.address,
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
                                                        subtitle = entry.label,
                                                        description = "Authenticate to reveal tracked Bitcoin address",
                                                    ),
                                                )
                                                revealedAddresses[entry.address] = result.visible
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Details") },
                                        onClick = {
                                            menuAddress = null
                                            detailsAddress = entry.address
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            menuAddress = null
                                            onEditClick(entry.address)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            menuAddress = null
                                            val found = addresses.find { it.address == entry.address }
                                            if (found != null) {
                                                scope.launch {
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "Deleted ${found.label.ifBlank { entry.address.redactedAddress() }}",
                                                        actionLabel = "Undo",
                                                        duration = SnackbarDuration.Long,
                                                    )
                                                    if (result != SnackbarResult.ActionPerformed) {
                                                        container.addressRepository.delete(found.address)
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
