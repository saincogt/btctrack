# Android Nav Restructure + Address CRUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the button-grid navigation with Material3 bottom tabs, then add in-app address management (add/edit/delete) with QR scan support.

**Architecture:** Phase 1 restructures `AppNavHost` into a three-tab bottom nav (`Overview / Addresses / Settings`) using a single NavController with nested nav graphs. Phase 2 adds address CRUD to the Addresses tab, including a `QrScanScreen` that uses CameraX + ZXing (no Google Play Services dependency, required for GrapheneOS).

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, Room, CameraX 1.3.x, ZXing (journeyapps 4.3.0 + zxing-core 3.5.3), Robolectric for unit tests.

---

## File map

### Phase 1 — Navigation

| Action | File |
|---|---|
| Modify | `app/src/main/java/com/zeal/btctrack/ui/navigation/Routes.kt` |
| Modify | `app/src/main/java/com/zeal/btctrack/ui/navigation/AppNavHost.kt` |
| Modify | `app/src/main/java/com/zeal/btctrack/ui/screens/DashboardScreen.kt` |
| Modify | `app/src/main/java/com/zeal/btctrack/ui/screens/SettingsScreen.kt` |

### Phase 2 — Data layer

| Action | File |
|---|---|
| Modify | `app/src/main/java/com/zeal/btctrack/data/local/dao/AddressDao.kt` |
| Modify | `app/src/main/java/com/zeal/btctrack/domain/repository/AddressRepository.kt` |
| Modify | `app/src/main/java/com/zeal/btctrack/data/repository/AddressRepositoryImpl.kt` |
| Create | `app/src/main/java/com/zeal/btctrack/domain/AddressValidator.kt` |
| Modify | `app/src/test/java/com/zeal/btctrack/data/repository/AddressRepositoryImplTest.kt` |
| Create | `app/src/test/java/com/zeal/btctrack/domain/AddressValidatorTest.kt` |

### Phase 2 — Screens

| Action | File |
|---|---|
| Modify (rename role) | `app/src/main/java/com/zeal/btctrack/ui/screens/GroupedAddressesScreen.kt` |
| Create | `app/src/main/java/com/zeal/btctrack/ui/screens/AddAddressScreen.kt` |
| Create | `app/src/main/java/com/zeal/btctrack/ui/screens/EditAddressScreen.kt` |
| Create | `app/src/main/java/com/zeal/btctrack/ui/screens/QrScanScreen.kt` |
| Create | `app/src/main/java/com/zeal/btctrack/ui/camera/QrCodeAnalyzer.kt` |

### Phase 2 — Build config

| Action | File |
|---|---|
| Modify | `app/build.gradle.kts` |
| Modify | `app/src/main/AndroidManifest.xml` |

---

## Task 1: Add `deleteByAddress` to AddressDao

**Files:**
- Modify: `app/src/main/java/com/zeal/btctrack/data/local/dao/AddressDao.kt`
- Test: `app/src/test/java/com/zeal/btctrack/data/local/AddressDaoTest.kt`

- [ ] **Step 1: Write the failing test**

Open `app/src/test/java/com/zeal/btctrack/data/local/AddressDaoTest.kt` and add this test at the end of the class (before the closing `}`):

```kotlin
@Test
fun `deleteByAddress removes only the targeted row`() = runBlocking {
    val keep = AddressEntity(
        id = "a", address = "addr-keep", label = "Keep", note = "",
        groupPath = "", orderValue = 1, watchOnly = true,
        createdAt = 1L, updatedAt = 1L,
    )
    val remove = AddressEntity(
        id = "b", address = "addr-remove", label = "Remove", note = "",
        groupPath = "", orderValue = 2, watchOnly = true,
        createdAt = 2L, updatedAt = 2L,
    )
    dao.upsertAll(listOf(keep, remove))
    dao.deleteByAddress("addr-remove")
    val remaining = dao.getAll()
    assertEquals(1, remaining.size)
    assertEquals("addr-keep", remaining.first().address)
}
```

- [ ] **Step 2: Run to confirm it fails**

```bash
cd /Users/zeal/Documents/study/projects/btctrack/android-app
./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.data.local.AddressDaoTest.deleteByAddress removes only the targeted row" 2>&1 | tail -20
```

Expected: compilation error — `deleteByAddress` does not exist yet.

- [ ] **Step 3: Add `deleteByAddress` to the DAO**

In `app/src/main/java/com/zeal/btctrack/data/local/dao/AddressDao.kt`, add after the `findByAddress` query:

```kotlin
@Query("DELETE FROM addresses WHERE address = :address")
suspend fun deleteByAddress(address: String)
```

- [ ] **Step 4: Run test to confirm it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.data.local.AddressDaoTest.deleteByAddress removes only the targeted row" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/data/local/dao/AddressDao.kt \
        app/src/test/java/com/zeal/btctrack/data/local/AddressDaoTest.kt
git commit -m "feat: add deleteByAddress to AddressDao"
```

---

## Task 2: Extend AddressRepository with add / update / delete

**Files:**
- Modify: `app/src/main/java/com/zeal/btctrack/domain/repository/AddressRepository.kt`
- Modify: `app/src/main/java/com/zeal/btctrack/data/repository/AddressRepositoryImpl.kt`
- Test: `app/src/test/java/com/zeal/btctrack/data/repository/AddressRepositoryImplTest.kt`

- [ ] **Step 1: Write the failing tests**

Add these three tests to `AddressRepositoryImplTest` (inside the class, before the closing `}`):

```kotlin
@Test
fun `add inserts a new entry observable via observeAll`() = runBlocking {
    val entry = AddressEntry(
        id = "new-1", address = "bc1qnew", label = "New",
        note = "test", groupPath = "Wallet", order = 5,
        watchOnly = true, createdAt = 100L, updatedAt = 100L,
    )
    repository.add(entry)
    val items = repository.observeAll().first()
    assertEquals(1, items.size)
    assertEquals("bc1qnew", items.first().address)
}

@Test
fun `update changes label and note of existing entry`() = runBlocking {
    val original = AddressEntry(
        id = "upd-1", address = "bc1qupdate", label = "Old label",
        note = "old note", groupPath = "", order = 1,
        watchOnly = true, createdAt = 10L, updatedAt = 10L,
    )
    repository.add(original)
    repository.update(original.copy(label = "New label", note = "new note", updatedAt = 99L))
    val item = repository.observeAll().first().first()
    assertEquals("New label", item.label)
    assertEquals("new note", item.note)
}

@Test
fun `delete removes entry by address`() = runBlocking {
    val entry = AddressEntry(
        id = "del-1", address = "bc1qdelete", label = "Del",
        note = "", groupPath = "", order = 1,
        watchOnly = true, createdAt = 10L, updatedAt = 10L,
    )
    repository.add(entry)
    repository.delete("bc1qdelete")
    val items = repository.observeAll().first()
    assertEquals(0, items.size)
}
```

- [ ] **Step 2: Run to confirm they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.data.repository.AddressRepositoryImplTest" 2>&1 | tail -20
```

Expected: compilation errors — `add`, `update`, `delete` not yet defined.

- [ ] **Step 3: Extend the interface**

Replace the contents of `app/src/main/java/com/zeal/btctrack/domain/repository/AddressRepository.kt`:

```kotlin
package com.zeal.btctrack.domain.repository

import com.zeal.btctrack.domain.model.AddressEntry
import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    fun observeAll(): Flow<List<AddressEntry>>
    suspend fun replaceAll(entries: List<AddressEntry>)
    suspend fun findByAddress(address: String): AddressEntry?
    suspend fun add(entry: AddressEntry)
    suspend fun update(entry: AddressEntry)
    suspend fun delete(address: String)
}
```

- [ ] **Step 4: Implement in AddressRepositoryImpl**

Replace the contents of `app/src/main/java/com/zeal/btctrack/data/repository/AddressRepositoryImpl.kt`:

```kotlin
package com.zeal.btctrack.data.repository

import com.zeal.btctrack.data.local.dao.AddressDao
import com.zeal.btctrack.data.mapper.toDomain
import com.zeal.btctrack.data.mapper.toEntity
import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.repository.AddressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AddressRepositoryImpl(
    private val addressDao: AddressDao,
) : AddressRepository {
    override fun observeAll(): Flow<List<AddressEntry>> =
        addressDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun replaceAll(entries: List<AddressEntry>) {
        addressDao.clearAll()
        addressDao.upsertAll(entries.map { it.toEntity() })
    }

    override suspend fun findByAddress(address: String): AddressEntry? =
        addressDao.findByAddress(address)?.toDomain()

    override suspend fun add(entry: AddressEntry) {
        addressDao.upsert(entry.toEntity())
    }

    override suspend fun update(entry: AddressEntry) {
        addressDao.upsert(entry.toEntity())
    }

    override suspend fun delete(address: String) {
        addressDao.deleteByAddress(address)
    }
}
```

- [ ] **Step 5: Run tests to confirm they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.data.repository.AddressRepositoryImplTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 4 tests passing.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/domain/repository/AddressRepository.kt \
        app/src/main/java/com/zeal/btctrack/data/repository/AddressRepositoryImpl.kt \
        app/src/test/java/com/zeal/btctrack/data/repository/AddressRepositoryImplTest.kt
git commit -m "feat: add add/update/delete to AddressRepository"
```

---

## Task 3: AddressValidator utility

**Files:**
- Create: `app/src/main/java/com/zeal/btctrack/domain/AddressValidator.kt`
- Create: `app/src/test/java/com/zeal/btctrack/domain/AddressValidatorTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/zeal/btctrack/domain/AddressValidatorTest.kt`:

```kotlin
package com.zeal.btctrack.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressValidatorTest {

    @Test
    fun `valid legacy P2PKH address passes`() {
        assertTrue(AddressValidator.isValid("1A1zP1eP5QGefi2DMPTfTL5SLmv7Divf"))
    }

    @Test
    fun `valid P2SH address passes`() {
        assertTrue(AddressValidator.isValid("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"))
    }

    @Test
    fun `valid native segwit bc1 address passes`() {
        assertTrue(AddressValidator.isValid("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"))
    }

    @Test
    fun `empty string fails`() {
        assertFalse(AddressValidator.isValid(""))
    }

    @Test
    fun `random text fails`() {
        assertFalse(AddressValidator.isValid("not-an-address"))
    }

    @Test
    fun `address with spaces fails`() {
        assertFalse(AddressValidator.isValid("bc1q ar0"))
    }
}
```

- [ ] **Step 2: Run to confirm they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.domain.AddressValidatorTest" 2>&1 | tail -10
```

Expected: compilation error — class does not exist.

- [ ] **Step 3: Create AddressValidator**

Create `app/src/main/java/com/zeal/btctrack/domain/AddressValidator.kt`:

```kotlin
package com.zeal.btctrack.domain

object AddressValidator {
    private val pattern = Regex("^(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,87}$")

    fun isValid(address: String): Boolean = address.trim().matches(pattern)
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.zeal.btctrack.domain.AddressValidatorTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 6 tests passing.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/domain/AddressValidator.kt \
        app/src/test/java/com/zeal/btctrack/domain/AddressValidatorTest.kt
git commit -m "feat: add AddressValidator for address format check"
```

---

## Task 4: Update Routes for bottom tab navigation

**Files:**
- Modify: `app/src/main/java/com/zeal/btctrack/ui/navigation/Routes.kt`

No tests for Routes — it is data only.

- [ ] **Step 1: Replace Routes.kt**

```kotlin
package com.zeal.btctrack.ui.navigation

sealed class Routes(val route: String) {
    // Tab roots (used by NavigationBar item selection)
    data object OverviewTab : Routes("overview_tab")
    data object AddressesTab : Routes("addresses_tab")
    data object SettingsTab : Routes("settings_tab")

    // Overview sub-routes
    data object Dashboard : Routes("dashboard")

    // Addresses sub-routes
    data object AddressList : Routes("address_list")
    data object AddAddress : Routes("add_address")
    data object EditAddress : Routes("edit_address/{address}") {
        fun withAddress(address: String) = "edit_address/$address"
    }
    data object QrScan : Routes("qr_scan")

    // Settings sub-routes
    data object Settings : Routes("settings_main")
    data object Import : Routes("import")
    data object Export : Routes("export")
}
```

- [ ] **Step 2: Confirm the project still compiles (build will fail on AppNavHost — expected)**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep -E "error:|warning:" | head -20
```

Expected: errors only in `AppNavHost.kt` (old Routes references) — that is correct, will be fixed in the next task.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/ui/navigation/Routes.kt
git commit -m "refactor: expand Routes for bottom tab navigation structure"
```

---

## Task 5: Rewrite AppNavHost with bottom tabs

**Files:**
- Modify: `app/src/main/java/com/zeal/btctrack/ui/navigation/AppNavHost.kt`

This task also removes `onBackClick` from `DashboardScreen` and `SettingsScreen` (they become tab roots) and removes nav-button callbacks from `DashboardScreen`.

- [ ] **Step 1: Update DashboardScreen signature**

Open `app/src/main/java/com/zeal/btctrack/ui/screens/DashboardScreen.kt`.

Change the function signature from:
```kotlin
fun DashboardScreen(
    container: AppContainer,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onGroupedListClick: () -> Unit,
    onSettingsClick: () -> Unit,
)
```
to:
```kotlin
fun DashboardScreen(
    container: AppContainer,
)
```

Remove the `Row` blocks that render the Import, Export, Groups, and Settings buttons (the two `Row` blocks at the bottom of the `Column`). Keep only `DashboardSummaryCard` and the Refresh/Reveal buttons row.

The final `Column` content in `DashboardScreen` should be:
```kotlin
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
```

Also remove the `TopAppBar` title back to just `"BTC Track"` (no change needed there).

- [ ] **Step 2: Update SettingsScreen signature**

Open `app/src/main/java/com/zeal/btctrack/ui/screens/SettingsScreen.kt`.

Change the function signature from:
```kotlin
fun SettingsScreen(
    container: AppContainer,
    onBackClick: () -> Unit,
)
```
to:
```kotlin
fun SettingsScreen(
    container: AppContainer,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
)
```

In the `Scaffold`'s `topBar`, remove the `navigationIcon` entirely:
```kotlin
topBar = { TopAppBar(title = { Text("Settings") }) },
```

At the end of the settings `Column` (after "Clear balance cache" button and the `Text(status)` line), add two navigation buttons:
```kotlin
Button(onClick = onImportClick, modifier = Modifier.fillMaxWidth()) {
    Text("Import addresses")
}
Button(onClick = onExportClick, modifier = Modifier.fillMaxWidth()) {
    Text("Export addresses")
}
```

- [ ] **Step 3: Update ImportScreen and ExportScreen callback names**

Open `app/src/main/java/com/zeal/btctrack/ui/screens/ImportScreen.kt`. Change `onBackClick` parameter name to `onBack` (keeps meaning clear):
```kotlin
fun ImportScreen(
    container: AppContainer,
    onBack: () -> Unit,
)
```
Update the `navigationIcon` inside to call `onBack()`.

Open `app/src/main/java/com/zeal/btctrack/ui/screens/ExportScreen.kt`. Same rename: `onBackClick` -> `onBack`.

- [ ] **Step 4: Rewrite AppNavHost**

Replace the entire contents of `app/src/main/java/com/zeal/btctrack/ui/navigation/AppNavHost.kt`:

```kotlin
package com.zeal.btctrack.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.ui.screens.AddAddressScreen
import com.zeal.btctrack.ui.screens.DashboardScreen
import com.zeal.btctrack.ui.screens.EditAddressScreen
import com.zeal.btctrack.ui.screens.ExportScreen
import com.zeal.btctrack.ui.screens.GroupedAddressesScreen
import com.zeal.btctrack.ui.screens.ImportScreen
import com.zeal.btctrack.ui.screens.QrScanScreen
import com.zeal.btctrack.ui.screens.SettingsScreen

private data class TabItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = listOf(
        TabItem(Routes.OverviewTab.route, "Overview", Icons.Default.Home),
        TabItem(Routes.AddressesTab.route, "Addresses", Icons.Default.List),
        TabItem(Routes.SettingsTab.route, "Settings", Icons.Default.Settings),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.OverviewTab.route,
            modifier = Modifier,
        ) {
            navigation(
                startDestination = Routes.Dashboard.route,
                route = Routes.OverviewTab.route,
            ) {
                composable(Routes.Dashboard.route) {
                    DashboardScreen(container = container)
                }
            }

            navigation(
                startDestination = Routes.AddressList.route,
                route = Routes.AddressesTab.route,
            ) {
                composable(Routes.AddressList.route) {
                    GroupedAddressesScreen(
                        container = container,
                        onAddClick = { navController.navigate(Routes.AddAddress.route) },
                        onEditClick = { address ->
                            navController.navigate(Routes.EditAddress.withAddress(address))
                        },
                    )
                }
                composable(Routes.AddAddress.route) { backStackEntry ->
                    val scannedAddress = backStackEntry.savedStateHandle
                        .getStateFlow<String?>("scanned_address", null)
                    AddAddressScreen(
                        container = container,
                        scannedAddressFlow = scannedAddress,
                        onBack = { navController.popBackStack() },
                        onScanQr = { navController.navigate(Routes.QrScan.route) },
                    )
                }
                composable(
                    route = Routes.EditAddress.route,
                    arguments = listOf(navArgument("address") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val address = backStackEntry.arguments?.getString("address") ?: return@composable
                    EditAddressScreen(
                        container = container,
                        address = address,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.QrScan.route) {
                    QrScanScreen(
                        onScanned = { address ->
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("scanned_address", address)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() },
                    )
                }
            }

            navigation(
                startDestination = Routes.Settings.route,
                route = Routes.SettingsTab.route,
            ) {
                composable(Routes.Settings.route) {
                    SettingsScreen(
                        container = container,
                        onImportClick = { navController.navigate(Routes.Import.route) },
                        onExportClick = { navController.navigate(Routes.Export.route) },
                    )
                }
                composable(Routes.Import.route) {
                    ImportScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.Export.route) {
                    ExportScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
```

Note: `AddAddressScreen`, `EditAddressScreen`, `QrScanScreen` do not exist yet — the import will cause compilation errors until those files are created (Tasks 9-13). That is expected.

- [ ] **Step 5: Update GroupedAddressesScreen signature (partial — just the function signature)**

Open `app/src/main/java/com/zeal/btctrack/ui/screens/GroupedAddressesScreen.kt`.

Change signature from:
```kotlin
fun GroupedAddressesScreen(
    container: AppContainer,
    onBackClick: () -> Unit,
)
```
to:
```kotlin
fun GroupedAddressesScreen(
    container: AppContainer,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
)
```

Keep the rest of the body unchanged for now — FAB and edit/delete will be added in Task 8. In the `Scaffold`'s `topBar`, remove the `navigationIcon`. Replace with:
```kotlin
topBar = { TopAppBar(title = { Text("Addresses") }) },
```

- [ ] **Step 6: Confirm compilation succeeds (except missing screen files)**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | grep -v "AddAddressScreen\|EditAddressScreen\|QrScanScreen" | head -20
```

Expected: only errors about the three new screen files that don't exist yet. No other errors.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/ui/navigation/Routes.kt \
        app/src/main/java/com/zeal/btctrack/ui/navigation/AppNavHost.kt \
        app/src/main/java/com/zeal/btctrack/ui/screens/DashboardScreen.kt \
        app/src/main/java/com/zeal/btctrack/ui/screens/SettingsScreen.kt \
        app/src/main/java/com/zeal/btctrack/ui/screens/ImportScreen.kt \
        app/src/main/java/com/zeal/btctrack/ui/screens/ExportScreen.kt \
        app/src/main/java/com/zeal/btctrack/ui/screens/GroupedAddressesScreen.kt
git commit -m "refactor: restructure navigation to bottom tab layout"
```

---

## Task 6: Add address dependencies + CAMERA permission

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add dependencies to build.gradle.kts**

In `app/build.gradle.kts`, add these lines inside the `dependencies { }` block, after the existing `implementation` lines:

```kotlin
implementation("androidx.compose.material:material-icons-core")
val cameraVersion = "1.3.4"
implementation("androidx.camera:camera-camera2:$cameraVersion")
implementation("androidx.camera:camera-lifecycle:$cameraVersion")
implementation("androidx.camera:camera-view:$cameraVersion")
implementation("com.journeyapps:zxing-android-embedded:4.3.0") { isTransitive = false }
implementation("com.google.zxing:core:3.5.3")
```

`material-icons-core` is managed by the Compose BOM already in scope — no version needed.

- [ ] **Step 2: Add CAMERA permission to AndroidManifest.xml**

In `app/src/main/AndroidManifest.xml`, add inside `<manifest>` before `<application>`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

- [ ] **Step 3: Sync and confirm Gradle resolves**

```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath 2>&1 | grep -E "zxing|camera" | head -10
```

Expected: lines showing `zxing-android-embedded:4.3.0`, `zxing:core:3.5.3`, `camera-camera2:1.3.4` etc.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "chore: add CameraX + ZXing dependencies, CAMERA permission"
```

---

## Task 7: QrCodeAnalyzer

**Files:**
- Create: `app/src/main/java/com/zeal/btctrack/ui/camera/QrCodeAnalyzer.kt`

No unit test — camera analysis requires a real image; manual verification covers this.

- [ ] **Step 1: Create the analyzer**

Create `app/src/main/java/com/zeal/btctrack/ui/camera/QrCodeAnalyzer.kt`:

```kotlin
package com.zeal.btctrack.ui.camera

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

class QrCodeAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader()

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }
        val yBuffer = mediaImage.planes[0].buffer
        val yBytes = ByteArray(yBuffer.remaining()).also { yBuffer.get(it) }
        val source = PlanarYUVLuminanceSource(
            yBytes,
            image.width,
            image.height,
            0, 0,
            image.width,
            image.height,
            false,
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            val result = reader.decode(bitmap)
            onResult(result.text)
        } catch (_: NotFoundException) {
            // No QR in this frame — continue scanning
        } finally {
            image.close()
        }
    }
}
```

- [ ] **Step 2: Confirm compilation**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | grep -v "AddAddressScreen\|EditAddressScreen\|QrScanScreen" | head -10
```

Expected: no errors except the three pending screen files.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/ui/camera/QrCodeAnalyzer.kt
git commit -m "feat: add QrCodeAnalyzer using ZXing + CameraX ImageAnalysis"
```

---

## Task 8: QrScanScreen

**Files:**
- Create: `app/src/main/java/com/zeal/btctrack/ui/screens/QrScanScreen.kt`

- [ ] **Step 1: Create QrScanScreen**

Create `app/src/main/java/com/zeal/btctrack/ui/screens/QrScanScreen.kt`:

```kotlin
package com.zeal.btctrack.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zeal.btctrack.ui.camera.QrCodeAnalyzer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(
    onScanned: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) permissionDenied = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR code") },
                navigationIcon = { Button(onClick = onCancel) { Text("Cancel") } },
            )
        },
    ) { innerPadding ->
        when {
            permissionDenied -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Camera permission denied.")
                    Text("Enter the address manually instead.")
                    Button(onClick = onCancel) { Text("Go back") }
                }
            }
            hasPermission -> {
                val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
                var scanned by remember { mutableStateOf(false) }

                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                @OptIn(ExperimentalGetImage::class)
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { ia ->
                                        ia.setAnalyzer(
                                            ContextCompat.getMainExecutor(ctx),
                                            QrCodeAnalyzer { result ->
                                                if (!scanned) {
                                                    scanned = true
                                                    onScanned(result)
                                                }
                                            }
                                        )
                                    }
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis,
                                )
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            else -> {
                // Waiting for permission result — show nothing
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }
        }
    }
}
```

- [ ] **Step 2: Confirm compilation (only AddAddressScreen + EditAddressScreen still missing)**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | grep -v "AddAddressScreen\|EditAddressScreen" | head -10
```

Expected: only errors about the two remaining screens.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/ui/screens/QrScanScreen.kt
git commit -m "feat: add QrScanScreen with CameraX + ZXing"
```

---

## Task 9: AddAddressScreen

**Files:**
- Create: `app/src/main/java/com/zeal/btctrack/ui/screens/AddAddressScreen.kt`

- [ ] **Step 1: Create AddAddressScreen**

Create `app/src/main/java/com/zeal/btctrack/ui/screens/AddAddressScreen.kt`:

```kotlin
package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
```

- [ ] **Step 2: Confirm only EditAddressScreen is still missing**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | head -10
```

Expected: only errors about `EditAddressScreen`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/ui/screens/AddAddressScreen.kt
git commit -m "feat: add AddAddressScreen with QR scan and manual input"
```

---

## Task 10: EditAddressScreen

**Files:**
- Create: `app/src/main/java/com/zeal/btctrack/ui/screens/EditAddressScreen.kt`

- [ ] **Step 1: Create EditAddressScreen**

Create `app/src/main/java/com/zeal/btctrack/ui/screens/EditAddressScreen.kt`:

```kotlin
package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.domain.model.AddressEntry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAddressScreen(
    container: AppContainer,
    address: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var entry by remember { mutableStateOf<AddressEntry?>(null) }
    var label by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }
    var order by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(address) {
        val found = container.addressRepository.findByAddress(address)
        if (found != null) {
            entry = found
            label = found.label
            group = found.groupPath
            order = if (found.order == 9999) "" else found.order.toString()
            note = found.note
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit address") },
                navigationIcon = { Button(onClick = onBack) { Text("Cancel") } },
            )
        },
    ) { innerPadding ->
        if (loading) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                Text("Loading...")
            }
            return@Scaffold
        }
        val current = entry
        if (current == null) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                Text("Address not found.")
                Button(onClick = onBack) { Text("Back") }
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = current.address,
                onValueChange = {},
                label = { Text("Bitcoin address") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                singleLine = true,
            )
            OutlinedTextField(
                value = label,
                onValueChange = { label = it; error = "" },
                label = { Text("Label (required)") },
                modifier = Modifier.fillMaxWidth(),
                isError = error.isNotEmpty(),
                supportingText = if (error.isNotEmpty()) ({ Text(error) }) else null,
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
                    if (label.isBlank()) {
                        error = "Label is required"
                    } else {
                        scope.launch {
                            container.addressRepository.update(
                                current.copy(
                                    label = label.trim(),
                                    groupPath = group.trim(),
                                    order = order.trim().toIntOrNull() ?: 9999,
                                    note = note.trim(),
                                    updatedAt = System.currentTimeMillis(),
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
```

- [ ] **Step 2: Confirm full compilation**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | head -10
```

Expected: no errors.

- [ ] **Step 3: Run full test suite**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`, all existing tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/ui/screens/EditAddressScreen.kt
git commit -m "feat: add EditAddressScreen for in-app address editing"
```

---

## Task 11: Add FAB + delete with undo to GroupedAddressesScreen

**Files:**
- Modify: `app/src/main/java/com/zeal/btctrack/ui/screens/GroupedAddressesScreen.kt`

- [ ] **Step 1: Rewrite GroupedAddressesScreen with FAB + long-press menu + snackbar undo**

Replace the entire file contents:

```kotlin
package com.zeal.btctrack.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.domain.model.AddressEntry
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupedAddressesScreen(
    container: AppContainer,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val biometricGate: BiometricGate? = remember(context) {
        context.findFragmentActivity()?.let(::AndroidBiometricGate)
    }
    val settings by container.settingsRepository.observe().collectAsStateCompat(null)
    val addresses by container.addressRepository.observeAll().collectAsStateCompat(emptyList())
    val balances by container.balanceRepository.observeAll().collectAsStateCompat(emptyList())
    val revealedAddresses = remember { mutableStateMapOf<String, Boolean>() }
    val revealStatuses = remember { mutableStateMapOf<String, String>() }
    var menuAddress by remember { mutableStateOf<String?>(null) }

    val sections = remember(addresses, balances) {
        val balancesByAddress = balances.associateBy { it.address }
        buildGroupedSections(addresses, balancesByAddress)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Addresses") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add address")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                Text("Tap + to add your first address.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { section ->
                    item { Text(section.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
                    items(section.items, key = { it.address }) { item ->
                        val visible = revealedAddresses[item.address] == true
                        Box {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { menuAddress = item.address },
                                    ),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(item.label, style = MaterialTheme.typography.titleMedium)
                                    Text(if (visible) item.address else item.address.redactedAddress())
                                    Text("Confirmed: ${item.confirmedSats} sats")
                                    Text("Tx count: ${item.txCount}")
                                    Text(revealStatuses[item.address] ?: "Address hidden")
                                }
                            }
                            DropdownMenu(
                                expanded = menuAddress == item.address,
                                onDismissRequest = { menuAddress = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Reveal / Hide address") },
                                    onClick = {
                                        menuAddress = null
                                        scope.launch {
                                            val requireBiometric = settings?.requireBiometricForDetails ?: true
                                            val gate = biometricGate ?: object : BiometricGate {
                                                override suspend fun authenticate(request: BiometricPromptRequest): BiometricGateResult =
                                                    BiometricGateResult(false, "Biometric unavailable")
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
                                        val snapshot = addresses.find { it.address == item.address }
                                        if (snapshot != null) {
                                            scope.launch {
                                                container.addressRepository.delete(snapshot.address)
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Deleted ${snapshot.label}",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short,
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    container.addressRepository.add(snapshot)
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Run full test suite**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Build debug APK**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, APK produced at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zeal/btctrack/ui/screens/GroupedAddressesScreen.kt
git commit -m "feat: add FAB and delete-with-undo to address list screen"
```

---

## Manual verification checklist

After installing the debug APK on the Pixel 8 (GrapheneOS):

- [ ] App opens to Overview tab showing Tor status + balance summary + Refresh/Reveal buttons
- [ ] Tapping Addresses tab shows address list (or empty state with "Tap + to add")
- [ ] Tapping + opens AddAddressScreen
- [ ] Tapping "Scan QR code" requests camera permission on first use
- [ ] Camera opens and decodes a Bitcoin address QR — back-fills address field
- [ ] Declining camera permission shows "Enter manually" fallback message
- [ ] Manually typing a valid bc1/1/3 address + label and tapping Save — address appears in list
- [ ] Typing an invalid address shows inline error "Invalid Bitcoin address"
- [ ] Long-press on address card shows dropdown with Reveal/Edit/Delete
- [ ] Edit opens EditAddressScreen with fields pre-filled, address field greyed out
- [ ] Saving edit reflects in list
- [ ] Delete shows Snackbar "Deleted [label] — Undo"
- [ ] Tapping Undo restores the address in the list
- [ ] Letting Snackbar expire permanently removes the address
- [ ] Settings tab shows Import and Export buttons at bottom
- [ ] Import + Export still work correctly
- [ ] Back stack within Addresses tab works (back from Add goes back to list, not to Overview)
- [ ] Switching tabs saves scroll state (addresses list position preserved)
