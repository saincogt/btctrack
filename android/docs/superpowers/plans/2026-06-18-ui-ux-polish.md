# UI/UX Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish BTC Track's dark-minimal UI across Dashboard, Addresses, and Settings screens.

**Architecture:** Four isolated changes — typography constants, then three screens. No new data flows or screens. All settings toggles switch to immediate-save. Task 1 must complete before Tasks 2-4 (they import the new text styles).

**Tech Stack:** Jetpack Compose, Material 3, existing `BtcTrackTheme`. Build: `./gradlew assembleDebug`. Install: `~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk` (or drag APK to device).

## Global Constraints

- minSdk 31, compileSdk 35
- No new dependencies
- All imports use full package paths (no star imports)
- Bitcoin Orange = `Color(0xFFF7931A)` (already in `Color.kt` as `BitcoinOrange`)
- TextSecondary = `Color(0xFF9A9A9A)` (already in `Color.kt`)
- `container.settingsRepository.update { ... }` is the pattern for saving settings

---

### Task 1: Typography constants

**Files:**
- Modify: `app/src/main/java/com/zeal/btctrack/ui/theme/Type.kt`

**Interfaces:**
- Produces:
  - `MonoBodyStyle: TextStyle` — monospace 14sp, for addresses and balance amounts in list rows
  - `MonoDisplayStyle: TextStyle` — monospace 38sp Bold, for Dashboard balance number
  - `SectionLabelStyle: TextStyle` — 11sp, letterSpacing 1.5sp, FontWeight.Medium, for section headers

- [ ] **Step 1: Replace `Type.kt` with the new content**

```kotlin
package com.zeal.btctrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MonoBodyStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
)

val MonoDisplayStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 38.sp,
    fontWeight = FontWeight.Bold,
)

val SectionLabelStyle = TextStyle(
    fontSize = 11.sp,
    letterSpacing = 1.5.sp,
    fontWeight = FontWeight.Medium,
)

val AppTypography = Typography()
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/ui/theme/Type.kt
git commit -m "feat: add MonoBodyStyle, MonoDisplayStyle, SectionLabelStyle typography constants"
```

---

### Task 2: DashboardScreen — balance chip + stats layout

**Files:**
- Modify: `app/src/main/java/com/zeal/btctrack/ui/screens/DashboardScreen.kt`

**Interfaces:**
- Consumes: `MonoDisplayStyle` from `com.zeal.btctrack.ui.theme`
- Consumes: `BitcoinOrange` from `com.zeal.btctrack.ui.theme`

**What changes:**
- Balance number: `MonoDisplayStyle` + `BitcoinOrange` color
- Unit chips (`[sats] [BTC]`) appear below the balance, outside the tap-to-toggle area, save immediately
- Bottom stats: uppercase label via `SectionLabelStyle`, value right-aligned

- [ ] **Step 1: Write the new DashboardScreen.kt**

Replace the file with:

```kotlin
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
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/ui/screens/DashboardScreen.kt
git commit -m "feat: dashboard balance in monospace orange, inline sats/BTC chip, uppercase stats"
```

---

### Task 3: GroupedAddressesScreen — flat rows + label-first display

**Files:**
- Modify: `app/src/main/java/com/zeal/btctrack/ui/screens/GroupedAddressesScreen.kt`

**Interfaces:**
- Consumes: `MonoBodyStyle`, `SectionLabelStyle` from `com.zeal.btctrack.ui.theme`
- Consumes: `BitcoinOrange` from `com.zeal.btctrack.ui.theme`

**What changes:**
- `Card` wrapper removed. Each address row is a flat `Column` with a `HorizontalDivider` below.
- Primary text: `item.label` if not blank (bodyMedium), else `"${addr.take(6)}...${addr.takeLast(4)}"` (MonoBodyStyle)
- Secondary text: when label is shown, also show truncated address below in MonoBodyStyle + TextSecondary
- Group header: SectionLabelStyle + BitcoinOrange color; title passed through `.uppercase()`
- Remove `Card` import and `CardDefaults` import

- [ ] **Step 1: Remove the Card/CardDefaults imports and add theme imports**

Find the import block at the top of `GroupedAddressesScreen.kt` (lines 1-73). Replace the Card-related imports and add the theme imports:

Remove these two lines:
```kotlin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
```

Add these lines (after the last `import androidx.compose.material3.*` line):
```kotlin
import com.zeal.btctrack.ui.theme.BitcoinOrange
import com.zeal.btctrack.ui.theme.MonoBodyStyle
import com.zeal.btctrack.ui.theme.SectionLabelStyle
```

- [ ] **Step 2: Update the group header item**

Find the group header item block (starting at `item(key = "header_${section.title}")`). Replace it with:

```kotlin
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
```

- [ ] **Step 3: Replace the address row Card with a flat Column**

Find the `items(section.items, ...)` block. Replace the `Box { Card(...) { Column { ... } } DropdownMenu }` structure with a flat layout. The old structure starts with `Box {` and `Card(`. Replace that entire item content with:

```kotlin
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
```

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

If there are "Unresolved reference" errors for `Card` or `CardDefaults`, verify Step 1 import changes were applied correctly.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/ui/screens/GroupedAddressesScreen.kt
git commit -m "feat: flat address rows, label-first display with monospace fallback, styled group headers"
```

---

### Task 4: SettingsScreen — immediate-save toggles + section grouping

**Files:**
- Modify: `app/src/main/java/com/zeal/btctrack/ui/screens/SettingsScreen.kt`

**Interfaces:**
- Consumes: `SectionLabelStyle` from `com.zeal.btctrack.ui.theme`

**What changes:**
- Remove `form` state (`SettingsFormState`), read directly from `settings`
- Remove `Save settings` Button and `status` Text
- All toggles save immediately via `container.settingsRepository.update { ... }`
- Add `SnackbarHost` to Scaffold for feedback on data actions
- Add section labels using `SectionLabelStyle`
- Replace import/export/clear Buttons with `ListItem` rows
- `balanceUnit` chip behavior unchanged (already saves immediately)

- [ ] **Step 1: Write the new SettingsScreen.kt**

Replace the entire file with:

```kotlin
package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.ui.collectAsStateCompat
import com.zeal.btctrack.ui.theme.SectionLabelStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onProxySettingsClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val settings by container.settingsRepository.observe().collectAsStateCompat(null)
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("GENERAL")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ToggleRow(
                    label = "Show balance by default",
                    checked = settings?.showTotalBalance ?: true,
                    onCheckedChange = { checked ->
                        scope.launch {
                            container.settingsRepository.update { it.copy(showTotalBalance = checked) }
                        }
                    },
                )
                ToggleRow(
                    label = "Tor required",
                    checked = settings?.torRequired ?: true,
                    onCheckedChange = { checked ->
                        scope.launch {
                            container.settingsRepository.update { it.copy(torRequired = checked) }
                        }
                    },
                )
            }

            HorizontalDivider()
            SectionHeader("DISPLAY")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Balance unit", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("sats", "BTC").forEach { unit ->
                        FilterChip(
                            selected = (settings?.balanceUnit ?: "sats") == unit,
                            onClick = {
                                scope.launch {
                                    container.settingsRepository.update { it.copy(balanceUnit = unit) }
                                }
                            },
                            label = { Text(unit) },
                        )
                    }
                }
            }

            HorizontalDivider()
            SectionHeader("NETWORK")
            ListItem(
                headlineContent = { Text("Proxy settings") },
                supportingContent = { Text("Tor SOCKS proxy and Esplora URL") },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onProxySettingsClick),
            )

            HorizontalDivider()
            SectionHeader("DATA")
            ListItem(
                headlineContent = { Text("Import addresses") },
                modifier = Modifier.clickable(onClick = onImportClick),
            )
            ListItem(
                headlineContent = { Text("Export addresses") },
                modifier = Modifier.clickable(onClick = onExportClick),
            )
            ListItem(
                headlineContent = {
                    Text("Clear balance cache", color = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.clickable {
                    scope.launch {
                        container.balanceRepository.clearAll()
                        snackbarHostState.showSnackbar("Balance cache cleared")
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = SectionLabelStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
```

- [ ] **Step 2: Remove unused `SettingsFormState` import if present**

Check if `SettingsFormState` is still imported in `SettingsScreen.kt`. If so, remove the import line:
```kotlin
import com.zeal.btctrack.ui.SettingsFormState
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/ui/screens/SettingsScreen.kt
git commit -m "feat: settings immediate-save toggles, section labels, ListItem actions, remove Save button"
```

---

### Task 5: Final build + deploy

- [ ] **Step 1: Full clean build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Install on device**

Run: `~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`

If adb is not at that path, find it with: `find ~/Library/Android -name "adb" -type f 2>/dev/null | head -3`

Alternative: drag the APK at `app/build/outputs/apk/debug/app-debug.apk` to the device via file manager.

- [ ] **Step 3: Visual verify Dashboard**

Open app. Check:
- Balance number is monospace, Bitcoin Orange
- `[sats] [BTC]` chips appear below balance (not inside tap area)
- Tapping balance area toggles show/hide without triggering chip
- "ADDRESSES" and "LAST REFRESH" labels are uppercase small-caps at bottom

- [ ] **Step 4: Visual verify Addresses**

Navigate to Addresses tab. Check:
- No card shadows — flat rows
- Group headers in uppercase Bitcoin Orange
- Address rows: label shown first if set, truncated address shown below label (or as primary if no label)
- Divider between rows

- [ ] **Step 5: Visual verify Settings**

Navigate to Settings. Check:
- Section headers: GENERAL, DISPLAY, NETWORK, DATA
- Toggling "Tor required" or "Show balance by default" saves immediately (no Save button)
- "Clear balance cache" shows snackbar confirmation

- [ ] **Step 6: Commit the final APK metadata if any changes**

```bash
git status
# only commit if there are meaningful source changes not yet committed
```
