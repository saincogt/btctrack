package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.domain.AddressValidator
import com.zeal.btctrack.domain.model.AddressEntry
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressScreen(
    container: AppContainer,
    scannedAddressFlow: StateFlow<String?>,
    onBack: () -> Unit,
    onScanQr: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scannedAddress by scannedAddressFlow.collectAsState()

    var address by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }
    var order by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(scannedAddress) {
        val s = scannedAddress
        if (!s.isNullOrEmpty()) address = s
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add address") },
                navigationIcon = { Button(onClick = onBack) { Text("Cancel") } },
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
            OutlinedTextField(
                value = address,
                onValueChange = { address = it; error = "" },
                label = { Text("Bitcoin address") },
                modifier = Modifier.fillMaxWidth(),
                isError = error.isNotEmpty(),
                supportingText = if (error.isNotEmpty()) ({ Text(error) }) else null,
                singleLine = true,
            )
            Button(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) {
                Text("Scan QR code")
            }
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label (required)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = group,
                onValueChange = { group = it },
                label = { Text("Group (e.g. Wallet/Account)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = order,
                onValueChange = { order = it },
                label = { Text("Order (optional, number)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val trimmed = address.trim()
                    when {
                        !AddressValidator.isValid(trimmed) -> error = "Invalid Bitcoin address"
                        label.isBlank() -> error = "Label is required"
                        else -> scope.launch {
                            val now = System.currentTimeMillis()
                            container.addressRepository.add(
                                AddressEntry(
                                    id = UUID.randomUUID().toString(),
                                    address = trimmed,
                                    label = label.trim(),
                                    note = note.trim(),
                                    groupPath = group.trim(),
                                    order = order.trim().toIntOrNull() ?: 9999,
                                    watchOnly = true,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            )
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}
