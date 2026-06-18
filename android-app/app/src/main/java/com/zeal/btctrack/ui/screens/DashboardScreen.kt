package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.R
import com.zeal.btctrack.ui.buildDashboardState
import com.zeal.btctrack.ui.collectAsStateCompat
import com.zeal.btctrack.ui.formatBalance
import com.zeal.btctrack.ui.theme.BitcoinOrange
import com.zeal.btctrack.ui.theme.MonoDisplayStyle
import com.zeal.btctrack.ui.theme.SectionLabelStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val addresses by container.addressRepository.observeAll().collectAsStateCompat(emptyList())
    val balances by container.balanceRepository.observeAll().collectAsStateCompat(emptyList())
    val settings by container.settingsRepository.observe().collectAsStateCompat(null)
    val torReachable by container.torReachable.collectAsStateCompat(false)

    var torStatus by remember { mutableStateOf("") }
    var showBalance by remember { mutableStateOf(false) }
    var balanceInitialized by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        torStatus = container.torHealthStatus()
    }

    LaunchedEffect(settings) {
        if (settings != null && !balanceInitialized) {
            showBalance = settings!!.showTotalBalance
            balanceInitialized = true
        }
    }

    val state = remember(addresses, balances, torStatus, showBalance, settings) {
        buildDashboardState(
            addresses = addresses,
            balances = balances,
            torStatus = torStatus,
            showBalance = showBalance,
            balanceUnit = settings?.balanceUnit ?: "sats",
            excludedGroups = settings?.excludedGroups ?: emptySet(),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BTC Track", fontWeight = FontWeight.SemiBold) },
                actions = {
                    Icon(
                        painter = painterResource(R.drawable.ic_tor_onion),
                        contentDescription = if (torReachable) "Tor online" else "Tor offline",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                            .alpha(if (torReachable) 1f else 0.25f),
                    )
                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                container.refreshAllTrackedAddresses()
                                torStatus = container.torHealthStatus()
                                isRefreshing = false
                            }
                        },
                        enabled = torReachable && !isRefreshing,
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = if (torReachable) MaterialTheme.colorScheme.onSurface
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(2f))

            // Tap-to-toggle balance area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBalance = !showBalance }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.showBalance) {
                    Text(
                        text = formatBalance(state.totalBalanceSats, state.balanceUnit),
                        style = MonoDisplayStyle,
                        color = BitcoinOrange,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = "• • •",
                        style = MonoDisplayStyle,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp,
                    )
                }
                Text(
                    text = if (state.showBalance) "Tap to hide" else "Tap to reveal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // Unit chips — outside tap area so tapping chip does not toggle balance
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                listOf("sats", "BTC").forEach { unit ->
                    FilterChip(
                        selected = state.balanceUnit == unit,
                        onClick = {
                            scope.launch {
                                container.settingsRepository.update { it.copy(balanceUnit = unit) }
                            }
                        },
                        label = { Text(unit, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Spacer(Modifier.weight(3f))

            // Stats — uppercase labels, right-aligned values
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StatRow("ADDRESSES", "${state.trackedCount}")
                StatRow("LAST REFRESH", state.lastRefreshLabel)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = SectionLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
