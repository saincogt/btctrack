# btctrack Android App Implementation Plan

> For Hermes: use this plan as the execution source for the Android app MVP. Prefer small, verifiable steps. Do not add widget work in MVP.

Goal: Build a privacy-first Android app for Google Pixel + GrapheneOS that tracks watch-only Bitcoin addresses using Tor-only access to mempool's onion API, while staying compatible with the current btctrack JSON formats.

Architecture: Build a native Kotlin Android app with Jetpack Compose UI, local persistence, Tor-only networking through an external SOCKS5 proxy such as Orbot, and biometric protection for sensitive views. Reuse the current btctrack privacy model: local-only address storage, no telemetry, randomized query order, randomized request delays, and no clearnet fallback.

Tech Stack: Kotlin, Jetpack Compose, Navigation Compose, Coroutines, OkHttp, kotlinx.serialization or Moshi, Room, DataStore, WorkManager, BiometricPrompt.

---

## 1. Product scope

### MVP includes
- Android app only
- Watch-only address tracking
- Local address book storage
- Tor-only onion API access via external SOCKS5 proxy
- Import from current CLI JSON and SwiftBar JSON
- Grouped address display
- Cached balances
- Manual refresh
- Background refresh with system constraints
- Biometric protection for sensitive details and balance reveal

### Not in MVP
- Home screen widget
- Price conversion / fiat value
- Push notifications
- Multi-chain support
- Cloud sync
- Electrum protocol
- Custom backend endpoint UI
- Embedded Tor runtime

---

## 2. Current repo compatibility constraints

### Existing input format A: CLI JSON
Source: `addresses.sample.json`

Example fields:
- `address`
- `label`
- `note`

### Existing input format B: SwiftBar JSON
Source: `plugins/.btcaddresses.sample.json`

Example fields:
- `address`
- `label`
- `group`
- `order`

### Existing privacy model to preserve
Source references:
- `README.md`
- `PRIVACY.md`
- `btctrack.py`
- `plugins/btctrack.1h.sh`

Rules to preserve:
- Tor + `.onion` only by default
- No automatic clearnet fallback in Android MVP
- Shuffle address refresh order each cycle
- Add random delay between requests
- Local-only storage
- Avoid obvious balance exposure on screen

---

## 3. Recommended repository layout

Create Android work under a new top-level directory:

```text
android-app/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  app/
    build.gradle.kts
    src/main/
      AndroidManifest.xml
      java/.../btctrack/
      res/

docs/
  plans/
    android-app-plan.md
  android-privacy.md
  android-import-export.md
```

Rationale:
- Keep Android work isolated from the current Python tools
- Preserve shared docs in the root `docs/`
- Leave room for future widget module without polluting the Python project layout

---

## 4. Internal domain model

### AddressEntry
```kotlin
data class AddressEntry(
    val id: String,
    val address: String,
    val label: String,
    val note: String,
    val groupPath: String,
    val order: Int,
    val watchOnly: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
```

### BalanceSnapshot
```kotlin
data class BalanceSnapshot(
    val address: String,
    val confirmedSats: Long,
    val unconfirmedSats: Long,
    val txCount: Int,
    val fetchedAt: Long,
    val source: String,
    val success: Boolean,
    val errorSummary: String?,
)
```

### AppSettings
```kotlin
data class AppSettings(
    val torRequired: Boolean,
    val socksHost: String,
    val socksPort: Int,
    val refreshIntervalMinutes: Int,
    val showTotalBalance: Boolean,
    val requireBiometricForDetails: Boolean,
    val requireBiometricForReveal: Boolean,
    val jitterMinMs: Long,
    val jitterMaxMs: Long,
)
```

### Import mapping rules

#### CLI JSON -> AddressEntry
- `address` -> `address`
- `label` -> `label`
- `note` -> `note`
- `groupPath` -> `""`
- `order` -> `9999`
- `watchOnly` -> `false`

#### SwiftBar JSON -> AddressEntry
- `address` -> `address`
- `label` -> `label`
- `group` -> `groupPath`
- `order` -> `order`
- `note` -> `""`
- `watchOnly` -> preserve if present, otherwise `false`

### Export targets
- Android native JSON
- CLI-compatible JSON
- SwiftBar-compatible JSON

---

## 5. Networking rules

### Endpoint
Use the existing onion address API pattern only:
- `http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/api/address/{address}`

### Transport
- External SOCKS5 proxy only
- Default proxy settings:
  - host: `127.0.0.1`
  - port: `9050`
- Expected runtime pairing: Orbot or another Tor SOCKS service

### Strict privacy behavior
- No automatic fallback to clearnet
- If proxy/Tor is unavailable, do not refresh remotely
- Keep old cache visible
- Surface clear UI status: `Tor unavailable`

### Refresh behavior
For each refresh cycle:
1. Load tracked addresses
2. Shuffle order
3. Insert random delay between requests
4. Fetch each address independently
5. Record per-address errors without aborting the full batch
6. Commit updated snapshots to local cache

### Logging behavior
- Never log full addresses in debug or release logs
- Log only redacted forms like `bc1qxy...9af2`
- Do not log full response bodies
- Do not integrate analytics or crash SDKs in MVP

---

## 6. UI scope

### Dashboard
Show:
- Tor status
- Last refresh time
- Number of tracked addresses
- Total balance card
- Reveal / hide balance action
- Refresh now
- Import action
- Navigate to grouped list
- Navigate to settings

### Grouped address list
Support:
- No group
- Single-level group like `Watching`
- Two-level path like `Trezor/HODL`

Display rules:
- Sort by `order`
- Then stable order inside group
- Use label first, truncated address second

### Address detail
Show:
- Full address
- Label
- Note
- Group path
- Order
- Confirmed balance
- Unconfirmed balance
- TX count
- Last fetched time
- Last error state

### Settings
Show:
- Tor-only explanation
- SOCKS host/port
- Refresh interval
- Jitter min/max
- Biometric protection toggles
- Import/export actions
- Clear cache action

---

## 7. Security model

### Biometric behavior
MVP defaults:
- Protect total balance reveal with biometric
- Protect address detail entry with biometric

If authentication fails:
- Stay in hidden state
- Do not navigate into sensitive detail content

### Screen privacy
Evaluate and likely enable `FLAG_SECURE` or equivalent sensitive-screen protection for:
- Address detail screen
- Revealed balance state

Decision note:
- If `FLAG_SECURE` harms usability too much, make it a setting in a later phase
- Do not block MVP on perfect secure-screen behavior

---

## 8. Storage model

### Use Room for
- Address entries
- Balance snapshots
- Optional refresh metadata if needed

### Use DataStore for
- Proxy host/port
- Refresh interval
- Jitter range
- Biometric toggles
- Display preferences

### Persistence rule
- Always show cached data first if available
- Network refresh updates cache after completion
- On refresh failure, keep old cache and update status only

---

## 9. Background refresh model

### Manual refresh
- Always available
- Primary refresh path for user trust

### Scheduled refresh
- Use WorkManager periodic work
- Default interval: 60 minutes
- Respect Android background constraints
- Do not promise exact execution timing

### Non-goals for MVP
- Realtime refresh
- High-frequency polling
- Notification pipeline

---

## 10. Phase plan

## Phase 0: Project bootstrap

Objective: Create the Android project skeleton and dependency baseline.

Files:
- Create: `android-app/settings.gradle.kts`
- Create: `android-app/build.gradle.kts`
- Create: `android-app/gradle.properties`
- Create: `android-app/app/build.gradle.kts`
- Create: `android-app/app/src/main/AndroidManifest.xml`
- Create: `android-app/app/src/main/java/.../btctrack/MainActivity.kt`
- Create: `android-app/app/src/main/java/.../btctrack/BtcTrackApp.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/navigation/AppNavHost.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/screens/DashboardScreen.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/screens/SettingsScreen.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/theme/*`

Success criteria:
- App builds in Android Studio / Gradle
- App installs on a Pixel / emulator
- Placeholder Dashboard and Settings screens are navigable

## Phase 1: Domain model and import pipeline

Objective: Define internal models and import current repo JSON formats.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/domain/model/AddressEntry.kt`
- Create: `android-app/app/src/main/java/.../btctrack/domain/model/BalanceSnapshot.kt`
- Create: `android-app/app/src/main/java/.../btctrack/domain/model/AppSettings.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/mapper/CliAddressJsonMapper.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/mapper/SwiftBarAddressJsonMapper.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/importer/AddressImportService.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/screens/ImportScreen.kt`
- Create: `android-app/app/src/test/java/.../btctrack/data/mapper/CliAddressJsonMapperTest.kt`
- Create: `android-app/app/src/test/java/.../btctrack/data/mapper/SwiftBarAddressJsonMapperTest.kt`

Success criteria:
- Can import `addresses.sample.json`
- Can import `plugins/.btcaddresses.sample.json`
- Imported entries preserve label/note/group/order as expected

## Phase 2: Persistence layer

Objective: Persist addresses, balance cache, and app settings locally.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/local/entity/AddressEntity.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/local/entity/BalanceSnapshotEntity.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/local/dao/AddressDao.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/local/dao/BalanceSnapshotDao.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/local/AppDatabase.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/local/SettingsStore.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/repository/AddressRepositoryImpl.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/repository/BalanceRepositoryImpl.kt`
- Create: `android-app/app/src/test/java/.../btctrack/data/local/AddressRepositoryTest.kt`

Success criteria:
- Imported data survives app restart
- Settings changes survive app restart
- Cached balances can be queried without network access

## Phase 3: Tor-only remote client

Objective: Fetch address data from the onion API through external SOCKS5 only.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/TorProxyConfig.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/TorHealthChecker.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/MempoolOnionApi.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/dto/AddressStatsDto.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/BalanceRemoteDataSource.kt`
- Create: `android-app/app/src/test/java/.../btctrack/data/remote/AddressStatsDtoParsingTest.kt`
- Create: `android-app/app/src/test/java/.../btctrack/data/remote/TorHealthCheckerTest.kt`

Success criteria:
- App detects Tor proxy misconfiguration cleanly
- App can fetch one known address through SOCKS5 + onion
- No clearnet request path exists in the Android remote layer

## Later phases
- Phase 4: Refresh engine with shuffle + jitter + cache writes
- Phase 5: Full UI screens and state handling
- Phase 6: Biometric gating
- Phase 7: WorkManager refresh scheduling and settings
- Phase 8: Export and interoperability
- Phase 9: Documentation and real-device validation

---

## 11. Detailed executable breakdown for Phases 0-3

### Phase 0: Project bootstrap

#### Task 0.1: Create Gradle root files
Objective: Initialize the standalone Android build.

Files:
- Create: `android-app/settings.gradle.kts`
- Create: `android-app/build.gradle.kts`
- Create: `android-app/gradle.properties`
- Create: `android-app/gradle/wrapper/gradle-wrapper.properties`
- Create or generate wrapper scripts: `android-app/gradlew`, `android-app/gradlew.bat`

Implementation notes:
- Use Kotlin DSL
- Define app module `:app`
- Pin Android Gradle Plugin and Kotlin plugin versions
- Set Java/Kotlin target consistently

Verification:
- Run from repo root: `cd android-app && ./gradlew tasks`
- Expected: Gradle lists tasks successfully

#### Task 0.2: Create app module and Android manifest
Objective: Create the installable Android app shell.

Files:
- Create: `android-app/app/build.gradle.kts`
- Create: `android-app/app/src/main/AndroidManifest.xml`

Implementation notes:
- Enable Compose
- Add minimum required permissions only:
  - `android.permission.INTERNET`
- Do not add analytics, ads, or cloud messaging dependencies

Verification:
- Run: `cd android-app && ./gradlew :app:assembleDebug`
- Expected: debug APK builds successfully

#### Task 0.3: Create base app entrypoint and navigation shell
Objective: Make the app launch and navigate between placeholder screens.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/MainActivity.kt`
- Create: `android-app/app/src/main/java/.../btctrack/BtcTrackApp.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/navigation/AppNavHost.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/navigation/Routes.kt`

Implementation notes:
- Use a single-activity Compose app
- Define initial routes:
  - dashboard
  - import
  - grouped_list
  - settings
- Use placeholder composables first

Verification:
- Run: `cd android-app && ./gradlew :app:installDebug`
- Manual check: app opens to Dashboard and can navigate to Settings

#### Task 0.4: Add placeholder screens and theme
Objective: Provide a visible skeleton that future phases can fill in.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/ui/screens/DashboardScreen.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/screens/SettingsScreen.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/screens/ImportScreen.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/screens/GroupedAddressesScreen.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/theme/Color.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/theme/Theme.kt`
- Create: `android-app/app/src/main/java/.../btctrack/ui/theme/Type.kt`

Implementation notes:
- Use a dark theme aligned with the current project aesthetic
- Display obvious placeholders and nav buttons only

Verification:
- Manual check on device/emulator: all placeholder screens render without crash

#### Task 0.5: Add dependency injection baseline or lightweight manual wiring
Objective: Keep construction clean before data layers arrive.

Files:
- Option A create: `android-app/app/src/main/java/.../btctrack/di/*`
- Option B keep manual wiring in `BtcTrackApp.kt`

Implementation notes:
- Prefer simplest path for MVP
- If using Hilt, add only if you are sure you want the added setup cost
- Manual constructor wiring is acceptable in Phase 0

Verification:
- Build still succeeds
- No unused framework complexity added yet

Phase 0 exit criteria:
- `assembleDebug` passes
- App launches
- Navigation shell works
- Only minimal permissions are present

---

### Phase 1: Domain model and import pipeline

#### Task 1.1: Define domain models
Objective: Lock the app's internal data contract before persistence and networking.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/domain/model/AddressEntry.kt`
- Create: `android-app/app/src/main/java/.../btctrack/domain/model/BalanceSnapshot.kt`
- Create: `android-app/app/src/main/java/.../btctrack/domain/model/AppSettings.kt`

Implementation notes:
- Keep them platform-agnostic
- Do not leak Room annotations into domain models

Verification:
- Build passes after model creation

#### Task 1.2: Create import DTOs for current JSON formats
Objective: Parse the two source formats explicitly rather than guessing field shapes inline.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/mapper/dto/CliAddressDto.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/mapper/dto/SwiftBarAddressDto.kt`

Implementation notes:
- Allow missing optional fields
- Consider lenient parsing for forward compatibility

Verification:
- Add compile-only parsing scaffolding

#### Task 1.3: Implement CLI JSON mapper
Objective: Convert current CLI JSON into internal models.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/mapper/CliAddressJsonMapper.kt`
- Create: `android-app/app/src/test/java/.../btctrack/data/mapper/CliAddressJsonMapperTest.kt`

Test cases:
- full entry with address/label/note
- empty label/note
- multiple entries
- stable default values for groupPath/order/watchOnly

Verification:
- Run: `cd android-app && ./gradlew :app:testDebugUnitTest --tests '*CliAddressJsonMapperTest'`
- Expected: tests pass

#### Task 1.4: Implement SwiftBar JSON mapper
Objective: Convert current SwiftBar JSON into internal models.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/mapper/SwiftBarAddressJsonMapper.kt`
- Create: `android-app/app/src/test/java/.../btctrack/data/mapper/SwiftBarAddressJsonMapperTest.kt`

Test cases:
- label/group/order parsing
- missing order defaults sanely
- one-level and two-level groups both preserved
- watchOnly preservation if field is later present

Verification:
- Run: `cd android-app && ./gradlew :app:testDebugUnitTest --tests '*SwiftBarAddressJsonMapperTest'`
- Expected: tests pass

#### Task 1.5: Build import service facade
Objective: Provide a single app-facing import entrypoint.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/importer/ImportFormat.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/importer/AddressImportService.kt`

Implementation notes:
- Support explicit format selection first
- Auto-detect format later if needed
- Return structured import result: count, warnings, parsed entries

Verification:
- Unit test service with both sample inputs

#### Task 1.6: Create Import screen UI shell
Objective: Wire the parsing flow to a visible screen before storage is added.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/ui/screens/ImportScreen.kt`

Implementation notes:
- MVP can use mock file content or bundled sample content first
- Real file picker can come after storage foundation if needed

Verification:
- Manual check: import screen can show a parsed preview list

Phase 1 exit criteria:
- Both current JSON formats parse into a single internal model
- Mapper tests pass
- Import screen can preview parsed entries

---

### Phase 2: Persistence layer

#### Task 2.1: Create Room entities
Objective: Define local storage schema separate from domain models.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/local/entity/AddressEntity.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/local/entity/BalanceSnapshotEntity.kt`

Implementation notes:
- Add indices for address lookup
- Decide primary key strategy early

Verification:
- Build passes with Room annotations

#### Task 2.2: Create DAOs
Objective: Expose address and balance storage operations.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/local/dao/AddressDao.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/local/dao/BalanceSnapshotDao.kt`

Operations needed:
- insert/update/delete addresses
- fetch all addresses sorted for display
- fetch latest snapshot by address
- fetch all latest snapshots
- clear cache

Verification:
- Add Room in-memory tests for DAO behavior

#### Task 2.3: Create AppDatabase
Objective: Register entities and DAOs in one Room database.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/local/AppDatabase.kt`

Verification:
- In-memory database test instantiates successfully

#### Task 2.4: Create SettingsStore with DataStore
Objective: Persist app preferences cleanly outside Room.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/local/SettingsStore.kt`

Settings to implement first:
- socksHost
- socksPort
- refreshIntervalMinutes
- showTotalBalance
- requireBiometricForDetails
- requireBiometricForReveal
- jitterMinMs
- jitterMaxMs

Verification:
- Unit/instrumentation test persists and reloads settings

#### Task 2.5: Create entity/domain mappers
Objective: Isolate conversion logic between Room and domain layers.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/mapper/AddressEntityMapper.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/mapper/BalanceSnapshotEntityMapper.kt`

Verification:
- Unit tests confirm round-trip integrity

#### Task 2.6: Create repositories
Objective: Expose persistence through domain-friendly interfaces.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/domain/repository/AddressRepository.kt`
- Create: `android-app/app/src/main/java/.../btctrack/domain/repository/BalanceRepository.kt`
- Create: `android-app/app/src/main/java/.../btctrack/domain/repository/SettingsRepository.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/repository/AddressRepositoryImpl.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/repository/BalanceRepositoryImpl.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/repository/SettingsRepositoryImpl.kt`

Verification:
- Repository tests using in-memory Room and test DataStore pass

#### Task 2.7: Wire import -> persistence flow
Objective: Move imported models into durable storage.

Files:
- Modify: `android-app/app/src/main/java/.../btctrack/ui/screens/ImportScreen.kt`
- Modify: `android-app/app/src/main/java/.../btctrack/data/importer/AddressImportService.kt`

Verification:
- Manual check: import entries, restart app, entries still visible

Phase 2 exit criteria:
- Address book persists across restarts
- Settings persist across restarts
- Balance cache schema is ready even before network is wired

---

### Phase 3: Tor-only remote client

#### Task 3.1: Create SOCKS proxy config model
Objective: Centralize proxy settings and validation.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/TorProxyConfig.kt`

Fields:
- host
- port
- enabled/required semantics if needed

Verification:
- Unit tests validate sane defaults and invalid ports

#### Task 3.2: Build OkHttp client configured for SOCKS5
Objective: Ensure all onion traffic goes through the proxy.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/OnionHttpClientFactory.kt`

Implementation notes:
- Build a `Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port))`
- Keep timeouts explicit
- Do not define alternate clearnet base URL in Android code

Verification:
- Unit test can inspect client configuration indirectly

#### Task 3.3: Implement Tor health checker
Objective: Fail fast when the SOCKS proxy is absent or misconfigured.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/TorHealthChecker.kt`
- Create: `android-app/app/src/test/java/.../btctrack/data/remote/TorHealthCheckerTest.kt`

Implementation notes:
- Start with a lightweight connectivity check
- Distinguish between:
  - proxy not reachable
  - onion request failed
  - unknown network failure

Verification:
- Tests cover expected failure classification

#### Task 3.4: Implement mempool onion DTOs and parser
Objective: Parse only the fields needed by current btctrack logic.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/dto/ChainStatsDto.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/dto/MempoolStatsDto.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/dto/AddressStatsDto.kt`
- Create: `android-app/app/src/test/java/.../btctrack/data/remote/AddressStatsDtoParsingTest.kt`

Required fields:
- `chain_stats.funded_txo_sum`
- `chain_stats.spent_txo_sum`
- `chain_stats.tx_count`
- `mempool_stats.funded_txo_sum`
- `mempool_stats.spent_txo_sum`

Verification:
- Parsing tests pass against saved sample JSON

#### Task 3.5: Implement remote data source for one address
Objective: Fetch and transform one address result into a domain snapshot payload.

Files:
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/MempoolOnionApi.kt`
- Create: `android-app/app/src/main/java/.../btctrack/data/remote/BalanceRemoteDataSource.kt`

Implementation notes:
- Compute confirmed and unconfirmed sats exactly as current Python code does
- Return structured result type, not raw exceptions only

Verification:
- Integration-style test with mocked response path
- If environment permits, manual real-device test through Orbot with one known address

#### Task 3.6: Add explicit no-fallback guardrails
Objective: Prevent accidental future regression to clearnet.

Files:
- Create: `android-app/app/src/test/java/.../btctrack/data/remote/NoClearnetFallbackTest.kt`
- Optionally add constants file: `android-app/app/src/main/java/.../btctrack/data/remote/OnionEndpoints.kt`

Implementation notes:
- Keep only onion endpoint constants
- Test that no clearnet base URL is used by remote fetch logic

Verification:
- Tests fail if a clearnet constant/path is introduced into remote fetch flow

Phase 3 exit criteria:
- One-address onion fetch works through SOCKS5
- Tor failures are surfaced clearly
- No automatic fallback path exists
- Remote parsing matches current Python balance math

---

## 12. Verification commands for implementation work

These are expected once the Android project exists.

```bash
cd android-app
./gradlew tasks
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
```

When device testing is available:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat | grep btctrack
```

---

## 13. Acceptance checklist

### Product acceptance
- App can import current btctrack JSON data
- App can fetch balances through Tor-only onion access
- App shows cached data and clear Tor status
- App protects sensitive views with biometric gates

### Privacy acceptance
- No automatic clearnet fallback
- No telemetry SDKs
- No cloud dependency
- No full-address logging

### Engineering acceptance
- Unit tests exist for import mapping and remote parsing
- Build is reproducible with Gradle
- Core flows are documented for GrapheneOS + Orbot

---

## 14. Immediate next execution target

When implementation starts, do this first:
1. Build `android-app/` skeleton
2. Verify Gradle build
3. Implement import mappers
4. Prove one-address SOCKS5 + onion fetch early

Reason: the highest-risk technical assumption is Android + external SOCKS5 + `.onion` compatibility. Prove that before polishing UI.
