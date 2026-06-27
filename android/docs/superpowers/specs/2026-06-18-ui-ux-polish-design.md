# UI/UX Polish Design

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Polish the BTC Track Android app UI from a raw demo feel to a clean dark-minimal financial tool.

**Architecture:** No new screens or data flows. Changes are confined to theme/typography and three existing screens. All settings toggles switch to immediate-save (no Save button).

**Tech Stack:** Jetpack Compose, Material 3, existing `BtcTrackTheme` / `DarkColors`.

---

## Design Decisions

### Visual Language
- Dark background (#0B0B0B), surface (#171717), variant (#242424) — already defined, keep as-is
- Bitcoin Orange (#F7931A) used only for: balance amount, group header text, interactive accent
- All other text: TextPrimary (#EAEAEA) or TextSecondary (#9A9A9A)
- No cards with elevation. Flat rows with HorizontalDivider separators.

### Typography
Add to `Type.kt`:
- `monoBody`: `FontFamily.Monospace`, 14sp — for truncated addresses and balance amounts in lists
- `monoDisplay`: `FontFamily.Monospace`, 38sp, Bold — for Dashboard balance
- `sectionLabel`: 11sp, `letterSpacing = 1.5.sp`, all-caps via code — for section headers in Settings and group headers in address list

### Components
All components remain Material 3 standard. No custom composables introduced.

---

## Screen Specs

### 1. DashboardScreen

**Balance area:**
- Single line: balance value in `monoDisplay` size, Bitcoin Orange color
- Immediately to the right (or below on overflow): `FilterChip` pair `[sats] [BTC]` — tapping switches unit AND saves to settings immediately (same behavior as current Settings chip)
- Below balance: "Tap to hide" / "Tap to reveal" in `bodySmall`, TextSecondary
- Hidden state: `• • •` in same size slot (no height jitter)

**Bottom stats:**
- Remove any card/surface container
- Two rows: `ADDRESSES` and `LAST REFRESH`
- Label: all-caps, 11sp, 1.5sp letter spacing, TextSecondary
- Value: `bodyMedium`, TextPrimary, right-aligned
- Pinned to bottom with `Spacer(Modifier.weight(1f))` above

**Tor indicator:** unchanged (Color.Unspecified tint, alpha for offline state)

### 2. GroupedAddressesScreen

**Group header row:**
- Label: section label style, Bitcoin Orange color
- Right side: group total balance, `monoBody`, TextSecondary
- Exclude toggle: `Visibility`/`VisibilityOff` icon button — unchanged behavior
- Separator: `HorizontalDivider` below header

**Address row (collapsed):**
- Primary text: label if non-blank, else truncated address (`address.take(6) + "..." + address.takeLast(4)`) — `monoBody` for address fallback, `bodyMedium` for label
- Secondary text (below primary): if label shown, also show truncated address in `monoBody`, TextSecondary
- Trailing: confirmed balance, `monoBody`, right-aligned; chevron icon
- Long-press context menu: unchanged (Edit, Delete, Details, Copy)

**Address row (expanded):**
- Shows confirmed balance only (already implemented)
- Refresh icon per-address: unchanged

### 3. SettingsScreen

**Remove:** `Save settings` button and associated `status` text state.

**Immediate-save all toggles:**
- `Tor required` toggle: save on change via `container.settingsRepository.update { it.copy(torRequired = it) }`
- `Show balance by default` toggle: save on change

**Section grouping** (use `sectionLabel` style + `HorizontalDivider`):

```
GENERAL
  Show balance by default  [toggle]
  Tor required             [toggle]

DISPLAY
  Balance unit             [sats chip] [BTC chip]

NETWORK
  Proxy settings  >

DATA
  Import addresses  (ListItem, clickable)
  Export addresses  (ListItem, clickable)
  Clear balance cache  (ListItem, clickable)
```

- Destructive actions (Clear cache) keep current behavior, no confirmation dialog needed
- Remove `status` Text — replace with `SnackbarHost` for feedback on data actions

---

## Out of Scope
- AddAddressScreen, EditAddressScreen, QrScanScreen, ImportScreen, ExportScreen — not touched
- Navigation structure — unchanged
- ProxySettingsScreen — unchanged
- Animations / transitions — not in this pass
- Bottom navigation — not in this pass
