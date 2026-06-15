package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.data.exporter.ExportFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var format by remember { mutableStateOf(ExportFormat.CLI_JSON) }
    var exportedJson by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Export locally for backup or interoperability.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export") },
                navigationIcon = { Button(onClick = onBack) { Text("Back") } },
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
            Text("Export tracked addresses as JSON")
            ExportFormatPicker(selected = format, onSelected = { format = it })
            Button(
                onClick = {
                    scope.launch {
                        status = runCatching {
                            exportedJson = container.exportAddressesUseCase(format)
                            "Exported to ${format.name}"
                        }.getOrElse { error -> "Export failed: ${error.message}" }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Generate export") }
            Text(status)
            OutlinedTextField(
                value = exportedJson,
                onValueChange = {},
                label = { Text("JSON output") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 12,
                readOnly = true,
            )
        }
    }
}

@Composable
private fun ExportFormatPicker(
    selected: ExportFormat,
    onSelected: (ExportFormat) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExportFormat.entries.forEach { item ->
            Button(onClick = { onSelected(item) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (selected == item) "✓ ${item.name}" else item.name)
            }
        }
    }
}
