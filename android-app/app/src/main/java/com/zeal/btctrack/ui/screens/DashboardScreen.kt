package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.R
import com.zeal.btctrack.ui.buildDashboardState
import com.zeal.btctrack.ui.collectAsStateCompat
import com.zeal.btctrack.ui.formatBalanceAmount
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
                    // 1st: eye toggle
                    IconButton(onClick = { showBalance = !showBalance }) {
                        Icon(
                            imageVector = if (showBalance) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (showBalance) "Hide balance" else "Show balance",
                            tint = if (showBalance) MaterialTheme.colorScheme.onSurface
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        )
                    }
                    // 2nd: tor indicator
                    Icon(
                        painter = painterResource(R.drawable.ic_tor_onion),
                        contentDescription = if (torReachable) "Tor online" else "Tor offline",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                            .alpha(if (torReachable) 1f else 0.25f),
                    )
                    // 3rd: refresh
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

            BalanceCard(
                totalSats = state.totalBalanceSats,
                balanceUnit = state.balanceUnit,
                showBalance = showBalance,
                onToggleVisibility = { showBalance = !showBalance },
                onToggleUnit = {
                    scope.launch {
                        val next = if ((settings?.balanceUnit ?: "sats") == "sats") "BTC" else "sats"
                        container.settingsRepository.update { it.copy(balanceUnit = next) }
                    }
                },
            )

            Spacer(Modifier.weight(3f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatRow("ADDRESSES", "${state.trackedCount}")
                StatRow("LAST REFRESH", state.lastRefreshLabel)
            }
        }
    }
}

@Composable
private fun BalanceCard(
    totalSats: Long,
    balanceUnit: String,
    showBalance: Boolean,
    onToggleVisibility: () -> Unit,
    onToggleUnit: () -> Unit,
) {
    val amountText = remember(totalSats, balanceUnit) {
        formatBalanceAmount(totalSats, balanceUnit)
    }
    val fontSize = when {
        amountText.length > 18 -> 20.sp
        amountText.length > 14 -> 26.sp
        amountText.length > 10 -> 32.sp
        else -> 38.sp
    }
    val iconSize = when {
        amountText.length > 18 -> 14.dp
        amountText.length > 14 -> 18.dp
        amountText.length > 10 -> 22.dp
        else -> 26.dp
    }

    // Full-width tap area so the ripple always has stable, fixed bounds regardless
    // of how the balance text width changes between units or show/hide states.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (showBalance) onToggleUnit else onToggleVisibility)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val unitAlpha = if (showBalance) 1f else 0.35f

            if (balanceUnit == "BTC") {
                Text(
                    text = "₿",
                    style = MonoDisplayStyle.copy(fontSize = fontSize),
                    color = BitcoinOrange.copy(alpha = unitAlpha),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_sats),
                    contentDescription = "sats",
                    modifier = Modifier.size(iconSize),
                    tint = BitcoinOrange.copy(alpha = unitAlpha),
                )
            }

            if (showBalance) {
                Text(
                    text = amountText,
                    style = MonoDisplayStyle.copy(fontSize = fontSize),
                    color = BitcoinOrange,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = "∗∗∗∗∗∗∗∗",
                    style = MonoDisplayStyle.copy(fontSize = fontSize, letterSpacing = 3.sp),
                    color = BitcoinOrange.copy(alpha = 0.35f),
                    maxLines = 1,
                )
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
