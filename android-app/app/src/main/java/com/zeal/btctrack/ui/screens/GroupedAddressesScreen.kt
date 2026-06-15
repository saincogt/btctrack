package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupedAddressesScreen(
    container: AppContainer,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val biometricGate: BiometricGate? = remember(context) {
        context.findFragmentActivity()?.let(::AndroidBiometricGate)
    }
    val settings by container.settingsRepository.observe().collectAsStateCompat(null)
    val addresses by container.addressRepository.observeAll().collectAsStateCompat(emptyList())
    val balances by container.balanceRepository.observeAll().collectAsStateCompat(emptyList())
    val revealedAddresses = remember { mutableStateMapOf<String, Boolean>() }
    val revealStatuses = remember { mutableStateMapOf<String, String>() }
    val sections = remember(addresses, balances) {
        val balancesByAddress = balances.associateBy { snapshot -> snapshot.address }
        buildGroupedSections(addresses, balancesByAddress)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Addresses") }) },
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
                Text("Import addresses first to see grouped lists.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { section ->
                    item { Text(section.title, style = MaterialTheme.typography.titleLarge) }
                    items(section.items, key = { it.address }) { item ->
                        val visible = revealedAddresses[item.address] == true
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(item.label, style = MaterialTheme.typography.titleMedium)
                                Text(if (visible) item.address else item.address.redactedAddress())
                                Text("Order: ${item.order}")
                                Text("Confirmed: ${item.confirmedSats} sats")
                                Text("Tx count: ${item.txCount}")
                                Text(revealStatuses[item.address] ?: "Address hidden")
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val requireBiometric = settings?.requireBiometricForDetails ?: true
                                            val gate = biometricGate ?: object : BiometricGate {
                                                override suspend fun authenticate(request: BiometricPromptRequest): BiometricGateResult {
                                                    return BiometricGateResult(false, "Biometric unavailable")
                                                }
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
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (visible) "Hide address" else "Reveal address")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
