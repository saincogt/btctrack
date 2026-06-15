# Android App v2 — Navigation Restructure + Address CRUD

Date: 2026-06-15

## Context

The current Android app (MVP) has a functional but rough UI: a Dashboard screen with a grid of buttons navigating to Import, Export, GroupedAddresses, and Settings. There is no in-app address management — addresses can only be added via JSON import.

Target device: Pixel 8 running GrapheneOS (no Google Play Services).

## Goals

1. Replace button-grid navigation with Material3 bottom tab navigation (3 tabs).
2. Allow users to add, edit, and delete individual addresses from within the app.
3. Support adding an address by QR scan or manual text input.

## Out of scope

- Fiat price conversion
- Transaction history per address
- Home screen widget
- Push notifications
- Embedded Tor runtime

---

## Phase 1 — Bottom Tab Navigation

### Navigation structure

```
BottomTabScaffold
  Tab 1: Overview
    DashboardScreen (existing, unchanged content)

  Tab 2: Addresses
    GroupedAddressListScreen (replaces GroupedAddressesScreen)
    AddAddressScreen
    EditAddressScreen
    QrScanScreen

  Tab 3: Settings
    SettingsScreen (existing)
    ImportScreen (moved from Dashboard)
    ExportScreen (moved from Dashboard)
```

Each tab has its own nested NavController so back-stack is per-tab (standard Material3 bottom nav pattern).

### Changes to existing screens

- `DashboardScreen`: remove Import / Export / Groups / Settings buttons. Keep Tor status, balance summary card, Refresh, and Reveal balance.
- `GroupedAddressesScreen`: rename to `GroupedAddressListScreen`, add FAB for adding address.
- `AppNavHost`: replace top-level NavHost with `BottomTabScaffold` + three nested NavHosts.
- `SettingsScreen`: add Import and Export entry points (e.g. two list items at bottom of settings list).

---

## Phase 2 — Address CRUD + QR Scan

### Add address flow

Entry: FAB on Addresses tab.

Fields:
- Address (required) — text input or QR scan result. Validated: must start with `bc1`, `1`, or `3`.
- Label (required) — free text display name.
- Group (optional) — format `Wallet/Account` or `Wallet`. Shown as hint text.
- Order (optional) — integer, lower = higher in list. Default 9999.
- Note (optional) — private memo.

QR scan: tapping "Scan" opens `QrScanScreen` (full-screen CameraX preview). On successful decode, screen closes and address field is populated. On failure or cancel, returns with no change.

### Edit address flow

Entry: long-press on address list item → context menu with "Edit" and "Delete".

Pre-populates all fields. Address field is read-only (shown greyed out). Save writes updated entry to Room.

### Delete address flow

Triggered from long-press context menu. Shows a Snackbar: "Deleted [label] — Undo" with a 3-second window. Undo restores the entry. After timeout, the record is permanently removed from Room.

### QR scan implementation

Library: `com.journeyapps:zxing-android-embedded` (pure Java, no Google Play Services dependency, Apache 2.0).
Camera: CameraX (`androidx.camera:camera-camera2`, `camera-lifecycle`, `camera-view`).
Permission: `android.permission.CAMERA`, requested at runtime on first scan attempt. If denied, the scan button is disabled and a message prompts manual input.

GrapheneOS compatibility: ZXing and CameraX are both AOSP-compatible with no Google Play Services dependency. ML Kit was rejected for this reason.

---

## Data layer

### AddressRepository additions

```
suspend fun add(entry: AddressEntry)
suspend fun update(entry: AddressEntry)
suspend fun delete(address: String)
```

Room DAO gets corresponding `@Insert(onConflict = REPLACE)`, `@Update`, and `@Delete(entity = AddressEntity::class)` by primary key.

Existing `observeAll()`, import, and export paths are unchanged.

---

## Testing strategy

Phase 1 (navigation restructure):
- All existing unit tests must continue to pass (no business logic changed).
- Manual verification: all three tabs reachable, back-stack per-tab behaves correctly, existing functionality (refresh, reveal, import, export) accessible via new entry points.

Phase 2 (CRUD):
- Unit tests for `AddressRepository.add/update/delete` using in-memory Room database.
- Unit tests for address field validation (format check).
- QR scan: manual verification only (camera cannot be simulated in CI).

Manual device verification checklist:
- Add address via manual input, confirm appears in grouped list.
- Add address via QR scan, confirm address field populated.
- Edit label/group/note, confirm list reflects change.
- Delete address, confirm Undo restores, confirm timeout removes permanently.
- Import JSON still works and does not conflict with CRUD.
- Export JSON reflects in-app changes.

---

## New dependencies

| Library | Version | License | Play Services required |
|---|---|---|---|
| `com.journeyapps:zxing-android-embedded` | 4.3.0 | Apache 2.0 | No |
| `androidx.camera:camera-camera2` | 1.3.x | Apache 2.0 | No |
| `androidx.camera:camera-lifecycle` | 1.3.x | Apache 2.0 | No |
| `androidx.camera:camera-view` | 1.3.x | Apache 2.0 | No |

No Firebase, no ML Kit, no Play Services APIs.

---

## Risk and rollback

Risk level: Medium (navigation refactor touches all screens; CRUD modifies Room schema).

Rollback: revert to previous commit. Room schema change requires a migration (version bump + `addMigrations`). If migration is not provided before release, `fallbackToDestructiveMigration` can be used during development only.

Mitigation: Phase 1 (nav) and Phase 2 (CRUD) are committed separately so each is independently revertable.
