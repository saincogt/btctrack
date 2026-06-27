# Hierarchical Address Tree Design

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the flat single-level group display with an arbitrary-depth collapsible tree, driven by `/`-separated `groupPath` values already stored in `AddressEntry`.

**Architecture:** Pure UI layer change. No database migration, no data model change. Tree is built at render time from existing `groupPath` strings. A new `AddressTree.kt` file holds tree logic; screens consume it. `excludedGroups` semantics change from exact-match to prefix-match, affecting balance totals on Dashboard.

**Tech Stack:** Jetpack Compose, Material 3, existing Room + DataStore. No new dependencies.

---

## Data Structure

### `AddressTreeNode` (new file: `app/src/main/java/com/zeal/btctrack/ui/AddressTree.kt`)

```kotlin
sealed class AddressTreeNode {
    data class Group(
        val path: String,             // full path, e.g. "冷钱包/Alex"
        val name: String,             // display segment, e.g. "Alex"
        val children: List<AddressTreeNode>,
        val totalSats: Long,          // sum of all descendant Leaf confirmedSats
    ) : AddressTreeNode()

    data class Leaf(
        val entry: AddressEntry,
        val confirmedSats: Long,
    ) : AddressTreeNode()
}
```

### `buildAddressTree(entries, balancesByAddress)`

- For each `AddressEntry`, split `groupPath` on `"/"` to get path segments
- Entries with blank `groupPath` become root-level `Leaf` nodes
- Intermediate path segments become `Group` nodes; groups at the same path are merged
- Each `Group.totalSats` = recursive sum of all descendant `Leaf.confirmedSats`
- Returns `List<AddressTreeNode>` sorted: groups before leaves at each level, then alphabetically by name/label

### `flattenVisible(nodes, expandState, depth)`

```kotlin
data class FlatItem(val node: AddressTreeNode, val depth: Int)

fun flattenVisible(
    nodes: List<AddressTreeNode>,
    expandState: Map<String, Boolean>,
    depth: Int = 0,
): List<FlatItem>
```

- Traverses the tree; includes a `Group`'s children only if `expandState[group.path] == true`
- `depth` starts at 0 for root nodes; increments by 1 per nesting level
- LazyColumn consumes this flat list directly

---

## Exclude Logic

### Current (exact match)
```kotlin
address.groupPath !in excludedGroups
```

### New (prefix match)
```kotlin
excludedGroups.none { p ->
    address.groupPath == p || address.groupPath.startsWith("$p/")
}
```

Toggling the exclude button on a `Group` node stores/removes `group.path` in `excludedGroups`. Toggling on a `Leaf` stores/removes `leaf.entry.groupPath`. This means:
- Excluding `"冷钱包"` excludes all addresses with groupPath `"冷钱包"`, `"冷钱包/Alex"`, `"冷钱包/Bob"`, etc.
- Excluding `"冷钱包/Alex"` excludes only Alex's addresses

`AppSettings.excludedGroups: Set<String>` type is unchanged.

### Dashboard balance impact
`buildDashboardState()` in `AppState.kt` already uses `excludedGroups` to filter addresses. Update the filter predicate to use prefix-match logic above. No other change needed.

---

## UI Rendering

### `GroupedAddressesScreen.kt`

Replace `buildGroupedSections()` call with `buildAddressTree()` + `flattenVisible()`.

The LazyColumn iterates `FlatItem` list:

**Group row (`AddressTreeNode.Group`):**
- Left padding: `depth * 16.dp`
- Expand/collapse icon: `▶` (collapsed) / `▼` (expanded); tap row to toggle
- Name: `group.name` in `SectionLabelStyle` + `BitcoinOrange` (or `onSurfaceVariant` if excluded)
- Right: `group.totalSats` formatted in `MonoBodyStyle` + `onSurfaceVariant`
- Eye icon button: toggles `group.path` in `excludedGroups`
- `HorizontalDivider` below the row

**Leaf row (`AddressTreeNode.Leaf`):**
- Left padding: `depth * 16.dp`
- Primary text: `entry.label` in `bodyMedium` if non-blank; else `"${addr.take(6)}...${addr.takeLast(4)}"` in `MonoBodyStyle`
- Secondary text (when label shown): truncated address in `MonoBodyStyle` + `onSurfaceVariant`
- Right: `confirmedSats` in `MonoBodyStyle` + refresh icon button
- Long-press: existing `DropdownMenu` (Reveal, Details, Edit, Delete) — unchanged
- Tap row: expands inline detail (address + copy, balance, updated) — same as current
- `HorizontalDivider` below the row

**Expand state:**
- `mutableStateMapOf<String, Boolean>()` keyed by `group.path`
- Default: all `false` (fully collapsed on first load)
- Leaf rows have no expand-key; their inline detail uses `mutableStateMapOf<String, Boolean>()` keyed by `entry.address` (same as current `expandedAddresses`)

---

## Compatibility

| Scenario | Behavior |
|----------|----------|
| Existing single-level groupPath (e.g. `"冷钱包"`) | Displays as single Group at depth 0 |
| Blank groupPath | Leaf at root depth 0, no group wrapping |
| Multi-level groupPath (e.g. `"冷钱包/Alex"`) | Group "冷钱包" at depth 0 → Group "Alex" at depth 1 → Leaf at depth 2 |
| SwiftBar import with nested group strings | Works as-is, no mapper change |
| Existing `excludedGroups` values | Semantics change: stored paths now act as prefix filters. Single-level paths behave identically to before (exact match is a subset of prefix match) |

---

## Out of Scope

- AddAddress/EditAddress screen: `groupPath` stays a plain text field — user types `冷钱包/Alex`
- No dedicated Group management screen
- No drag-to-reorder within groups
- No collapse-all / expand-all button
- ProxySettingsScreen, DashboardScreen layout, Settings — not touched
