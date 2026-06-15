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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.zeal.btctrack.data.importer.ImportFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rawJson by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(ImportFormat.CLI_JSON) }
    var resultMessage by remember { mutableStateOf("Paste CLI JSON or SwiftBar JSON to import.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import") },
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
            Text("Import tracked addresses", style = MaterialTheme.typography.titleLarge)
            Text("Supported formats: CLI JSON and SwiftBar JSON.")
            FormatPicker(selected = format, onSelected = { format = it })
            OutlinedTextField(
                value = rawJson,
                onValueChange = { rawJson = it },
                label = { Text("JSON payload") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 10,
            )
            Button(
                onClick = {
                    scope.launch {
                        resultMessage = runCatching {
                            val result = container.importAddressesUseCase(rawJson, format)
                            "Imported ${result.importedCount} entries. warnings=${result.warnings.size}"
                        }.getOrElse { error ->
                            "Import failed: ${error.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Import now")
            }
            Text(resultMessage)
        }
    }
}

@Composable
private fun FormatPicker(
    selected: ImportFormat,
    onSelected: (ImportFormat) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(ImportFormat.CLI_JSON, ImportFormat.SWIFTBAR_JSON).forEach { format ->
            Button(onClick = { onSelected(format) }, modifier = Modifier.fillMaxWidth()) {
                RadioButton(selected = selected == format, onClick = null)
                Text(format.name)
            }
        }
    }
}
