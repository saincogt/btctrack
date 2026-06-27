# Hierarchical Address Tree Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace flat single-level group display with an arbitrary-depth collapsible tree driven by `/`-separated `groupPath` values in `AddressEntry`.

**Architecture:** Pure UI layer change. No DB migration, no data model change. New `AddressTree.kt` holds all tree logic. `GroupedAddressesScreen.kt` is rewritten to consume `FlatItem` lists. The exclude logic in `AppState.kt` changes from exact-match to prefix-match. `GroupedAddressSection`, `GroupedAddressRow`, and `buildGroupedSections` in `UiModels.kt` are deleted.

**Tech Stack:** Jetpack Compose, Material 3, JUnit4 unit tests (no mocks, pure functions).

## Global Constraints

- minSdk 31, compileSdk 35, Kotlin, `material-icons-extended` already in deps
- `AddressEntry.groupPath`: blank = no group (root leaf), `/`-separated for nesting
- `AddressTreeNode.Group.path`: full path string, e.g. `"Cold/Alex"`; `.name`: last segment only
- `FlatItem.depth`: 0 for root nodes, increments by 1 per level
- Default expand state: all collapsed (`groupExpandState` empty = all false)
- Groups before leaves at each level; groups sorted alphabetically; leaves sorted by `order`, then `label`, then `address`
- `BalanceSnapshot` fields: `address`, `confirmedSats`, `unconfirmedSats`, `txCount`, `fetchedAt`, `source`, `success`, `errorSummary`
- `AddressEntry` fields: `id`, `address`, `label`, `note`, `groupPath`, `order`, `watchOnly`, `createdAt`, `updatedAt`
- Test command: `./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.ui.*" -p /Users/zeal/Documents/study/projects/btctrack/android-app`
- Build command: `./gradlew assembleDebug -p /Users/zeal/Documents/study/projects/btctrack/android-app`

---

### Task 1: AddressTree.kt — data structures, tree builder, flatten

**Files:**
- Create: `app/src/main/java/com/zeal/btctrack/ui/AddressTree.kt`
- Create: `app/src/test/java/com/zeal/btctrack/ui/AddressTreeTest.kt`

**Interfaces:**
- Produces:
  - `sealed class AddressTreeNode` with `Group(path, name, children, totalSats)` and `Leaf(entry, confirmedSats)`
  - `data class FlatItem(val node: AddressTreeNode, val depth: Int)`
  - `fun buildAddressTree(entries: List<AddressEntry>, balancesByAddress: Map<String, BalanceSnapshot>): List<AddressTreeNode>`
  - `fun flattenVisible(nodes: List<AddressTreeNode>, expandState: Map<String, Boolean>, depth: Int = 0): List<FlatItem>`

- [ ] **Step 1: Write the failing tests**

File: `app/src/test/java/com/zeal/btctrack/ui/AddressTreeTest.kt`

```kotlin
package com.zeal.btctrack.ui

import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.model.BalanceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressTreeTest {

    private fun entry(address: String, groupPath: String, label: String = "", order: Int = 0) = AddressEntry(
        id = address, address = address, label = label, note = "",
        groupPath = groupPath, order = order, watchOnly = false,
        createdAt = 0L, updatedAt = 0L,
    )

    private fun snap(address: String, sats: Long) = BalanceSnapshot(
        address = address, confirmedSats = sats, unconfirmedSats = 0L,
        txCount = 0, fetchedAt = 0L, source = "test",
        success = true, errorSummary = null,
    )

    @Test
    fun `blank groupPath becomes root leaf`() {
        val tree = buildAddressTree(listOf(entry("addr1", "")), emptyMap())
        assertEquals(1, tree.size)
        assert(tree[0] is AddressTreeNode.Leaf)
    }

    @Test
    fun `single-segment groupPath becomes root group containing a leaf`() {
        val tree = buildAddressTree(listOf(entry("addr1", "Cold")), emptyMap())
        assertEquals(1, tree.size)
        val group = tree[0] as AddressTreeNode.Group
        assertEquals("Cold", group.path)
        assertEquals("Cold", group.name)
        assertEquals(1, group.children.size)
        assert(group.children[0] is AddressTreeNode.Leaf)
    }

    @Test
    fun `two-segment groupPath builds two-level tree`() {
        val tree = buildAddressTree(listOf(entry("addr1", "Cold/Alex")), emptyMap())
        assertEquals(1, tree.size)
        val root = tree[0] as AddressTreeNode.Group
        assertEquals("Cold", root.path)
        assertEquals("Cold", root.name)
        val child = root.children[0] as AddressTreeNode.Group
        assertEquals("Cold/Alex", child.path)
        assertEquals("Alex", child.name)
        assert(child.children[0] is AddressTreeNode.Leaf)
    }

    @Test
    fun `totalSats aggregates all descendant leaf sats`() {
        val entries = listOf(entry("addr1", "Cold/Alex"), entry("addr2", "Cold/Bob"))
        val balances = mapOf("addr1" to snap("addr1", 100L), "addr2" to snap("addr2", 50L))
        val tree = buildAddressTree(entries, balances)
        val root = tree[0] as AddressTreeNode.Group
        assertEquals(150L, root.totalSats)
        val alex = root.children[0] as AddressTreeNode.Group
        val bob = root.children[1] as AddressTreeNode.Group
        assertEquals(100L, alex.totalSats)
        assertEquals(50L, bob.totalSats)
    }

    @Test
    fun `entries with same parent group are merged`() {
        val entries = listOf(entry("addr1", "Cold"), entry("addr2", "Cold"))
        val tree = buildAddressTree(entries, emptyMap())
        assertEquals(1, tree.size)
        val group = tree[0] as AddressTreeNode.Group
        assertEquals(2, group.children.size)
    }

    @Test
    fun `groups appear before leaves at same level`() {
        val entries = listOf(
            entry("addr1", "Cold"),
            entry("addr2", "Cold/Alex"),
            entry("addr3", ""),
        )
        val tree = buildAddressTree(entries, emptyMap())
        assert(tree[0] is AddressTreeNode.Group)
        assert(tree[1] is AddressTreeNode.Leaf)
        val coldGroup = tree[0] as AddressTreeNode.Group
        assert(coldGroup.children[0] is AddressTreeNode.Group)
        assert(coldGroup.children[1] is AddressTreeNode.Leaf)
    }

    @Test
    fun `flattenVisible returns only root items when all collapsed`() {
        val entries = listOf(entry("addr1", "Cold/Alex"))
        val tree = buildAddressTree(entries, emptyMap())
        val flat = flattenVisible(tree, emptyMap())
        assertEquals(1, flat.size)
        assertEquals(0, flat[0].depth)
        assert(flat[0].node is AddressTreeNode.Group)
    }

    @Test
    fun `flattenVisible includes children when group is expanded`() {
        val entries = listOf(entry("addr1", "Cold"))
        val tree = buildAddressTree(entries, emptyMap())
        val flat = flattenVisible(tree, mapOf("Cold" to true))
        assertEquals(2, flat.size)
        assertEquals(0, flat[0].depth)
        assertEquals(1, flat[1].depth)
        assert(flat[0].node is AddressTreeNode.Group)
        assert(flat[1].node is AddressTreeNode.Leaf)
    }

    @Test
    fun `flattenVisible increments depth for each nesting level`() {
        val entries = listOf(entry("addr1", "Cold/Alex"))
        val tree = buildAddressTree(entries, emptyMap())
        val flat = flattenVisible(tree, mapOf("Cold" to true, "Cold/Alex" to true))
        assertEquals(3, flat.size)
        assertEquals(0, flat[0].depth)
        assertEquals(1, flat[1].depth)
        assertEquals(2, flat[2].depth)
    }

    @Test
    fun `collapsed group does not show children even if nested groups are expanded`() {
        val entries = listOf(entry("addr1", "A/B/C"))
        val tree = buildAddressTree(entries, emptyMap())
        val flat = flattenVisible(tree, mapOf("A/B" to true, "A/B/C" to true))
        assertEquals(1, flat.size)
    }
}
```

- [ ] **Step 2: Run tests — expect failure (symbol not found)**

```
./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.ui.AddressTreeTest" -p /Users/zeal/Documents/study/projects/btctrack/android-app
```

Expected: compile error — `buildAddressTree`, `flattenVisible`, `AddressTreeNode` not defined.

- [ ] **Step 3: Create `AddressTree.kt`**

File: `app/src/main/java/com/zeal/btctrack/ui/AddressTree.kt`

```kotlin
package com.zeal.btctrack.ui

import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.model.BalanceSnapshot

sealed class AddressTreeNode {
    data class Group(
        val path: String,
        val name: String,
        val children: List<AddressTreeNode>,
        val totalSats: Long,
    ) : AddressTreeNode()

    data class Leaf(
        val entry: AddressEntry,
        val confirmedSats: Long,
    ) : AddressTreeNode()
}

data class FlatItem(val node: AddressTreeNode, val depth: Int)

fun buildAddressTree(
    entries: List<AddressEntry>,
    balancesByAddress: Map<String, BalanceSnapshot>,
): List<AddressTreeNode> = buildNodes("", entries, balancesByAddress)

private fun buildNodes(
    parentPath: String,
    allEntries: List<AddressEntry>,
    balancesByAddress: Map<String, BalanceSnapshot>,
): List<AddressTreeNode> {
    val directLeaves = allEntries
        .filter { it.groupPath == parentPath }
        .sortedWith(compareBy({ it.order }, { it.label }, { it.address }))
        .map { entry ->
            AddressTreeNode.Leaf(
                entry = entry,
                confirmedSats = balancesByAddress[entry.address]?.confirmedSats ?: 0L,
            )
        }

    val childEntries = if (parentPath.isEmpty()) {
        allEntries.filter { it.groupPath.isNotBlank() }
    } else {
        allEntries.filter { it.groupPath.startsWith("$parentPath/") }
    }

    val childGroups = childEntries
        .groupBy { entry ->
            val rest = if (parentPath.isEmpty()) entry.groupPath
                       else entry.groupPath.substring(parentPath.length + 1)
            rest.substringBefore("/")
        }
        .entries
        .sortedBy { it.key }
        .map { (segment, _) ->
            val fullPath = if (parentPath.isEmpty()) segment else "$parentPath/$segment"
            val children = buildNodes(fullPath, allEntries, balancesByAddress)
            AddressTreeNode.Group(
                path = fullPath,
                name = segment,
                children = children,
                totalSats = sumLeafSats(children),
            )
        }

    return childGroups + directLeaves
}

private fun sumLeafSats(nodes: List<AddressTreeNode>): Long = nodes.sumOf { node ->
    when (node) {
        is AddressTreeNode.Group -> node.totalSats
        is AddressTreeNode.Leaf -> node.confirmedSats
    }
}

fun flattenVisible(
    nodes: List<AddressTreeNode>,
    expandState: Map<String, Boolean>,
    depth: Int = 0,
): List<FlatItem> {
    val result = mutableListOf<FlatItem>()
    for (node in nodes) {
        result.add(FlatItem(node, depth))
        if (node is AddressTreeNode.Group && expandState[node.path] == true) {
            result.addAll(flattenVisible(node.children, expandState, depth + 1))
        }
    }
    return result
}
```

- [ ] **Step 4: Run tests — expect all 9 to pass**

```
./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.ui.AddressTreeTest" -p /Users/zeal/Documents/study/projects/btctrack/android-app
```

Expected: `BUILD SUCCESSFUL`, 9 tests passed.

- [ ] **Step 5: Commit**

```bash
git -C /Users/zeal/Documents/study/projects/btctrack/android-app add \
  app/src/main/java/com/zeal/btctrack/ui/AddressTree.kt \
  app/src/test/java/com/zeal/btctrack/ui/AddressTreeTest.kt
git -C /Users/zeal/Documents/study/projects/btctrack/android-app commit -m "feat: add AddressTree data structures with builder and flatten"
```

---

### Task 2: Update exclude logic in AppState.kt

**Files:**
- Modify: `app/src/main/java/com/zeal/btctrack/ui/AppState.kt` (lines 62-65)
- Create: `app/src/test/java/com/zeal/btctrack/ui/AppStateExcludeTest.kt`

**Interfaces:**
- Consumes: `buildDashboardState` (existing function signature — unchanged)
- Change: filter predicate in `buildDashboardState` switches from exact-match to prefix-match

- [ ] **Step 1: Write the failing tests**

File: `app/src/test/java/com/zeal/btctrack/ui/AppStateExcludeTest.kt`

```kotlin
package com.zeal.btctrack.ui

import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.model.BalanceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStateExcludeTest {

    private fun entry(address: String, groupPath: String) = AddressEntry(
        id = address, address = address, label = "", note = "",
        groupPath = groupPath, order = 0, watchOnly = false,
        createdAt = 0L, updatedAt = 0L,
    )

    private fun snap(address: String, sats: Long) = BalanceSnapshot(
        address = address, confirmedSats = sats, unconfirmedSats = 0L,
        txCount = 0, fetchedAt = 1000L, source = "test",
        success = true, errorSummary = null,
    )

    @Test
    fun `exact match excluded`() {
        val state = buildDashboardState(
            addresses = listOf(entry("a1", "Cold"), entry("a2", "Exchange")),
            balances = listOf(snap("a1", 100L), snap("a2", 50L)),
            torStatus = "", showBalance = true,
            excludedGroups = setOf("Cold"),
        )
        assertEquals(50L, state.totalBalanceSats)
    }

    @Test
    fun `parent path excludes child paths`() {
        val state = buildDashboardState(
            addresses = listOf(
                entry("a1", "Cold"),
                entry("a2", "Cold/Alex"),
                entry("a3", "Exchange"),
            ),
            balances = listOf(snap("a1", 100L), snap("a2", 200L), snap("a3", 50L)),
            torStatus = "", showBalance = true,
            excludedGroups = setOf("Cold"),
        )
        assertEquals(50L, state.totalBalanceSats)
    }

    @Test
    fun `child path does not exclude sibling or parent`() {
        val state = buildDashboardState(
            addresses = listOf(
                entry("a1", "Cold"),
                entry("a2", "Cold/Alex"),
                entry("a3", "Cold/Bob"),
            ),
            balances = listOf(snap("a1", 100L), snap("a2", 200L), snap("a3", 50L)),
            torStatus = "", showBalance = true,
            excludedGroups = setOf("Cold/Alex"),
        )
        assertEquals(150L, state.totalBalanceSats)
    }

    @Test
    fun `blank groupPath address included when only named paths excluded`() {
        val state = buildDashboardState(
            addresses = listOf(entry("a1", ""), entry("a2", "Cold")),
            balances = listOf(snap("a1", 100L), snap("a2", 50L)),
            torStatus = "", showBalance = true,
            excludedGroups = setOf("Cold"),
        )
        assertEquals(100L, state.totalBalanceSats)
    }

    @Test
    fun `no excluded groups includes all`() {
        val state = buildDashboardState(
            addresses = listOf(entry("a1", "Cold"), entry("a2", "Exchange")),
            balances = listOf(snap("a1", 100L), snap("a2", 50L)),
            torStatus = "", showBalance = true,
            excludedGroups = emptySet(),
        )
        assertEquals(150L, state.totalBalanceSats)
    }
}
```

- [ ] **Step 2: Run tests — expect failure on parent-path test**

```
./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.ui.AppStateExcludeTest" -p /Users/zeal/Documents/study/projects/btctrack/android-app
```

Expected: `parent path excludes child paths` fails; others may pass or fail.

- [ ] **Step 3: Update `AppState.kt` lines 62-65**

In `app/src/main/java/com/zeal/btctrack/ui/AppState.kt`, replace:

```kotlin
    val includedAddressSet = addresses
        .filter { it.groupPath.ifBlank { "No group" } !in excludedGroups }
        .map { it.address }
        .toSet()
```

With:

```kotlin
    val includedAddressSet = addresses
        .filter { entry ->
            excludedGroups.none { p -> entry.groupPath == p || entry.groupPath.startsWith("$p/") }
        }
        .map { it.address }
        .toSet()
```

- [ ] **Step 4: Run tests — expect all 5 to pass**

```
./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.ui.AppStateExcludeTest" -p /Users/zeal/Documents/study/projects/btctrack/android-app
```

Expected: `BUILD SUCCESSFUL`, 5 tests passed.

- [ ] **Step 5: Commit**

```bash
git -C /Users/zeal/Documents/study/projects/btctrack/android-app add \
  app/src/main/java/com/zeal/btctrack/ui/AppState.kt \
  app/src/test/java/com/zeal/btctrack/ui/AppStateExcludeTest.kt
git -C /Users/zeal/Documents/study/projects/btctrack/android-app commit -m "fix: change excludedGroups filter to prefix-match for nested paths"
```

---

### Task 3: Rewrite GroupedAddressesScreen + clean UiModels

**Files:**
- Modify: `app/src/main/java/com/zeal/btctrack/ui/screens/GroupedAddressesScreen.kt` (full rewrite)
- Modify: `app/src/main/java/com/zeal/btctrack/ui/UiModels.kt` (remove `GroupedAddressSection`, `GroupedAddressRow`, `buildGroupedSections`)

**Interfaces:**
- Consumes (from Task 1): `AddressTreeNode`, `FlatItem`, `buildAddressTree`, `flattenVisible`
- Leaf row: uses `node.entry.*` instead of `item.*` (the old `GroupedAddressRow` fields)
- Group row: new `GroupNodeRow` inline block rendering expand/collapse + eye toggle

**Note on expand state reactivity:** `groupExpandState` is a `SnapshotStateMap`. Calling `flattenVisible(tree, groupExpandState)` directly in the composable body (not inside `remember`) establishes snapshot reads on the map. Compose recomposes the screen when any key changes, recomputing `flatItems` automatically.

- [ ] **Step 1: Rewrite `GroupedAddressesScreen.kt`**

Replace the entire file with:

```kotlin
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
```

- [ ] **Step 2: Trim `UiModels.kt` — remove the three deleted symbols**

`GroupedAddressSection`, `GroupedAddressRow`, and `buildGroupedSections` are now unused. Replace the entire file with only what remains:

```kotlin
package com.zeal.btctrack.ui

fun String.redactedAddress(): String =
    if (length <= 12) this else "${take(6)}...${takeLast(4)}"
```

- [ ] **Step 3: Build to verify no compile errors**

```
./gradlew assembleDebug -p /Users/zeal/Documents/study/projects/btctrack/android-app
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run all UI unit tests**

```
./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.ui.*" -p /Users/zeal/Documents/study/projects/btctrack/android-app
```

Expected: all tests pass (AddressTreeTest + AppStateExcludeTest + any pre-existing tests).

- [ ] **Step 5: Commit**

```bash
git -C /Users/zeal/Documents/study/projects/btctrack/android-app add \
  app/src/main/java/com/zeal/btctrack/ui/screens/GroupedAddressesScreen.kt \
  app/src/main/java/com/zeal/btctrack/ui/UiModels.kt
git -C /Users/zeal/Documents/study/projects/btctrack/android-app commit -m "feat: rewrite address list as collapsible tree with depth-based indentation"
```

---

### Task 4: Build APK and verify

**Files:** None modified.

- [ ] **Step 1: Full release-debug build**

```
./gradlew assembleDebug -p /Users/zeal/Documents/study/projects/btctrack/android-app
```

Expected: `BUILD SUCCESSFUL`. APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Run full test suite**

```
./gradlew :app:testDebugUnitTest -p /Users/zeal/Documents/study/projects/btctrack/android-app
```

Expected: `BUILD SUCCESSFUL` with no test failures.

- [ ] **Step 3: Install on device (if adb available)**

```bash
~/Library/Android/sdk/platform-tools/adb install -r \
  /Users/zeal/Documents/study/projects/btctrack/android-app/app/build/outputs/apk/debug/app-debug.apk
```

If adb not available, report APK path for manual install.

- [ ] **Step 4: Manual verification checklist**

Golden path (record in report):
1. Open Addresses tab — all groups collapsed by default
2. Tap a group row — it expands, children appear indented
3. Tap the group row again — it collapses
4. Tap a nested group (2+ levels deep) — verify depth indentation increases
5. Eye icon on a group — balance disappears from Dashboard total
6. Tap a leaf row — inline detail expands showing address, balance, updated time
7. Long-press a leaf row — context menu appears (Reveal, Details, Edit, Delete)
8. Dashboard shows only non-excluded groups' balances

- [ ] **Step 5: Commit if any build fixes were needed; otherwise report done**
