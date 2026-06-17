package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.ui.DashboardUiState
import com.zeal.btctrack.ui.buildDashboardState
import com.zeal.btctrack.ui.collectAsStateCompat
import com.zeal.btctrack.ui.security.AndroidBiometricGate
import com.zeal.btctrack.ui.security.BiometricGate
import com.zeal.btctrack.ui.security.BiometricPromptRequest
import com.zeal.btctrack.ui.security.SensitiveRevealController
import com.zeal.btctrack.ui.security.findFragmentActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    container: AppContainer,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val addresses by container.addressRepository.observeAll().collectAsStateCompat(emptyList())
    val balances by container.balanceRepository.observeAll().collectAsStateCompat(emptyList())
    val settings by container.settingsRepository.observe().collectAsStateCompat(null)
    val biometricGate: BiometricGate? = remember(context) {
        context.findFragmentActivity()?.let(::AndroidBiometricGate)
    }
    val torReachable by container.torReachable.collectAsStateCompat(false)
    var torStatus by remember { mutableStateOf("Checking Tor...") }
    var showBalance by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var revealStatus by remember { mutableStateOf("Balance hidden") }

    LaunchedEffect(Unit) {
        torStatus = container.torHealthStatus()
    }

    val state = remember(addresses, balances, torStatus, showBalance) {
        buildDashboardState(addresses, balances, torStatus, showBalance)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("BTC Track") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DashboardSummaryCard(state = state, isRefreshing = isRefreshing, revealStatus = revealStatus)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isRefreshing = true
                            container.refreshAllTrackedAddresses()
                            torStatus = container.torHealthStatus()
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isRefreshing && torReachable,
                ) {
                    Text(if (isRefreshing) "Refreshing..." else "Refresh now")
                }
                Button(
                    onClick = {
                        scope.launch {
                            val requireBiometric = settings?.requireBiometricForReveal ?: true
                            val gate = biometricGate ?: object : BiometricGate {
                                override suspend fun authenticate(request: BiometricPromptRequest) =
                                    com.zeal.btctrack.ui.security.BiometricGateResult(
                                        success = false,
                                        message = "Biometric unavailable",
                                    )
                            }
                            val result = SensitiveRevealController(gate).toggle(
                                currentlyVisible = showBalance,
                                requireBiometric = requireBiometric,
                                request = BiometricPromptRequest(
                                    title = "Reveal total balance",
                                    subtitle = "BTC Track privacy protection",
                                    description = "Authenticate to reveal cached balance totals",
                                ),
                            )
                            showBalance = result.visible
                            revealStatus = result.message
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (showBalance) "Hide balance" else "Reveal balance")
                }
            }

        }
    }
}

@Composable
private fun DashboardSummaryCard(
    state: DashboardUiState,
    isRefreshing: Boolean,
    revealStatus: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Dashboard", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tor status: ${state.torStatus}")
            Text("Tracked addresses: ${state.trackedCount}")
            Text("Last refresh: ${state.lastRefreshLabel}")
            Text(
                if (state.showBalance) "Total balance: ${state.totalBalanceSats} sats"
                else "Total balance: hidden"
            )
            Text("Reveal status: $revealStatus")
            if (isRefreshing) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Refreshing cached balances...")
            }
        }
    }
}
