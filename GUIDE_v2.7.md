# btctrack v2.7 — 3-Level Hierarchy & Custom Ordering

## ✨ What's New

### Before v2.7 (2-Level)
```
Group → Address
```
Example menu:
```
Cold Wallet HODL
  0.50000000 BTC
  └─ Address 1
  └─ Address 2
  
Trezor Alex  
  0.30000000 BTC
  └─ Address 3
  
Trezor HODL
  0.80000000 BTC
  └─ Address 4
```

**Problems:**
- Groups sorted alphabetically only
- No way to organize related groups together
- Flat structure becomes messy with many addresses

---

### After v2.7 (3-Level + Custom Order)
```
Wallet → Account → Address
```
Example menu:
```
Trezor                          [order=1, displayed first]
  1.10000000 BTC
  
  Alex                          [account under Trezor]
    0.30000000 BTC
    └─ Address 3
    
  HODL                          [account under Trezor]
    0.80000000 BTC
    └─ Address 4

---

Cold Wallet                     [order=2]
  0.50000000 BTC
  
  HODL                          [account under Cold Wallet]
    0.50000000 BTC
    └─ Address 1
    └─ Address 2
```

**Benefits:**
- ✅ Related accounts grouped under wallets
- ✅ Control display order (important wallets first)
- ✅ Clear visual hierarchy
- ✅ Per-wallet and per-account totals
- ✅ Backward compatible (old configs still work)

---

## 🚀 Quick Start

### Option 1: Use Old Format (Still Works)
```json
{
  "address": "bc1q...",
  "label": "My Address",
  "group": "Personal"
}
```

### Option 2: Upgrade to 3-Level
```json
{
  "address": "bc1q...",
  "label": "My Address",
  "group": "Trezor/Personal",
  "order": 1
}
```

**Change:**
1. Add `/` between Wallet and Account: `"Personal"` → `"Trezor/Personal"`
2. Add `"order"` field to control sorting (optional)

---

## 📋 Field Reference

| Field | Required | Description | Example |
|-------|----------|-------------|---------|
| `address` | ✅ Yes | Bitcoin address | `"bc1q..."` |
| `label` | ❌ No | Display name | `"Cold Storage"` |
| `group` | ❌ No | Hierarchy path | `"Trezor/HODL"` |
| `order` | ❌ No | Display priority (lower = first) | `1` |

### `group` Format Options

| Format | Example | Result |
|--------|---------|--------|
| Empty | `""` or omit | Ungrouped (bottom) |
| Wallet only | `"Personal"` | 2-level: Personal → Address |
| Wallet/Account | `"Trezor/HODL"` | 3-level: Trezor → HODL → Address |

### `order` Values

| Value | Meaning | Use Case |
|-------|---------|----------|
| `1-10` | High priority | Main wallets (Trezor, Ledger) |
| `10-50` | Medium priority | Secondary wallets (Cold storage) |
| `50-90` | Low priority | Temporary/testing |
| `99` | Very low | Watching addresses |
| `9999` (default) | Lowest | No preference (alphabetical) |

---

## 💡 Real-World Examples

### Example 1: Your Current Structure (Migrated)

**Before (v2.4):**
```json
[
  {"address": "bc1q...", "label": "Urga Zeus", "group": "Trezor Default"},
  {"address": "bc1q...", "label": "Change", "group": "Trezor Default"},
  {"address": "bc1q...", "label": "Alex", "group": "Trezor Alex"},
  {"address": "bc1q...", "label": "Sancho W006", "group": "Trezor HODL"},
  {"address": "bc1q...", "label": "CW Personal", "group": "Cold Wallet HODL"},
  {"address": "bc1q...", "label": "Hos", "group": "Darwin Mongol"}
]
```

**After (v2.7):**
```json
[
  {"address": "bc1q...", "label": "Urga Zeus", "group": "Trezor/Default", "order": 1},
  {"address": "bc1q...", "label": "Change", "group": "Trezor/Default", "order": 1},
  {"address": "bc1q...", "label": "Alex", "group": "Trezor/Alex", "order": 1},
  {"address": "bc1q...", "label": "Sancho W006", "group": "Trezor/HODL", "order": 1},
  {"address": "bc1q...", "label": "CW Personal", "group": "Cold Wallet/HODL", "order": 2},
  {"address": "bc1q...", "label": "Hos", "group": "Darwin/Mongol", "order": 3}
]
```

**Display:**
```
Trezor                          [order=1]
  2.50000000 BTC
  
  Alex
    0.15000000 BTC
    └─ Alex  0.03000000 BTC
    └─ Alex  0.05000000 BTC
    └─ (6 more addresses)
    
  Default
    0.80000000 BTC
    └─ Urga Zeus  0.30000000 BTC
    └─ Change  0.50000000 BTC
    
  HODL
    1.55000000 BTC
    └─ Sancho W006  0.10000000 BTC
    └─ (5 more addresses)

---

Cold Wallet                     [order=2]
  1.30000000 BTC
  
  HODL
    1.30000000 BTC
    └─ CW Personal  0.50000000 BTC
    └─ (6 more addresses)

---

Darwin                          [order=3]
  0.50000000 BTC
  
  Mongol
    0.50000000 BTC
    └─ Hos  0.05000000 BTC
    └─ (9 more addresses)
```

### Example 2: Multiple Hardware Wallets

```json
[
  {"address": "bc1q...", "group": "Trezor/Main", "order": 1},
  {"address": "bc1q...", "group": "Trezor/Backup", "order": 1},
  {"address": "bc1q...", "group": "Ledger/Main", "order": 2},
  {"address": "bc1q...", "group": "Ledger/Multisig", "order": 2},
  {"address": "bc1q...", "group": "Software/Hot Wallet", "order": 10},
  {"address": "bc1q...", "group": "Watching", "order": 99}
]
```

**Display order:**
1. Trezor (order=1) → Main, Backup
2. Ledger (order=2) → Main, Multisig
3. Software (order=10) → Hot Wallet
4. Watching (order=99)

### Example 3: By Purpose

```json
[
  {"address": "bc1q...", "group": "HODL/Long-term", "order": 1},
  {"address": "bc1q...", "group": "HODL/Emergency Fund", "order": 1},
  {"address": "bc1q...", "group": "Trading/Active", "order": 5},
  {"address": "bc1q...", "group": "Trading/Reserved", "order": 5},
  {"address": "bc1q...", "group": "Income/Mining", "order": 10},
  {"address": "bc1q...", "group": "Income/Staking", "order": 10}
]
```

---

## 🔄 Migration Steps

### Step 1: Backup Your Config
```bash
cp plugins/.btcaddresses.json plugins/.btcaddresses.json.backup
```

### Step 2: Edit the Config
```bash
# macOS: open in TextEdit
open -e plugins/.btcaddresses.json

# Or from SwiftBar menu: ✎ Edit Config → TextEdit
```

### Step 3: Apply Changes

**Find & Replace Pattern:**

| Find This | Replace With | Notes |
|-----------|--------------|-------|
| `"group": "Trezor Default"` | `"group": "Trezor/Default", "order": 1` | Add `/` + order |
| `"group": "Trezor Alex"` | `"group": "Trezor/Alex", "order": 1` | Same wallet, different account |
| `"group": "Cold Wallet HODL"` | `"group": "Cold Wallet/HODL", "order": 2` | Space becomes wallet name |
| `"group": "Darwin Mongol"` | `"group": "Darwin/Mongol", "order": 3` | Both words become hierarchy |

**General pattern:**
```
"Group Name"  →  "Wallet/Account", "order": N
```

### Step 4: Test
- Save the file
- SwiftBar will auto-refresh (or click "Refresh")
- Check the menu dropdown structure

### Step 5: Adjust Order Values
- Change `order` values to prioritize important wallets
- Lower number = appears first
- Save and refresh to see changes

---

## 🛠️ Troubleshooting

### Issue: Addresses not showing up
**Solution:** Check JSON syntax with:
```bash
python3 -m json.tool plugins/.btcaddresses.json
```

### Issue: Order not working as expected
**Check:**
1. `order` must be a number, not a string: `"order": 1` ✅ not `"order": "1"` ❌
2. Lower number appears first: `order=1` before `order=10`

### Issue: Want to mix 2-level and 3-level
**Answer:** It works! You can have:
```json
[
  {"address": "...", "group": "Simple"},           // 2-level
  {"address": "...", "group": "Trezor/HODL"}      // 3-level
]
```

### Issue: How to remove hierarchy?
**Options:**
1. Use wallet name only: `"group": "Personal"`
2. Leave empty: `"group": ""`
3. Omit field entirely

---

## 📚 Additional Resources

- [MIGRATION_v2.7.md](MIGRATION_v2.7.md) — Detailed migration guide
- [.btcaddresses.sample.v2.7.json](plugins/.btcaddresses.sample.v2.7.json) — Example config
- [CHANGELOG.md](CHANGELOG.md) — Version history

---

## ❓ FAQ

**Q: Do I have to upgrade?**  
A: No. Your v2.4 config works without changes. Upgrade when ready.

**Q: Can I still use spaces in group names?**  
A: Yes. `"group": "Cold Wallet HODL"` works (treated as wallet name). For hierarchy, use `/`: `"Cold Wallet/HODL"`.

**Q: What if two wallets have the same order?**  
A: They're sorted alphabetically within the same order value.

**Q: How to reset to default order?**  
A: Remove all `"order"` fields or set them to `9999`.

**Q: Does this affect privacy?**  
A: No. Query order is still randomized with delays. Display order is separate.

**Q: Can accounts have different orders under the same wallet?**  
A: Yes. Each account can have its own `order` value.

**Q: Maximum order value?**  
A: No limit, but recommend 1-99 for clarity.

---

**Version:** v2.7  
**Date:** 2026-03-01  
**Author:** btctrack team
