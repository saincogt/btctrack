# Migration Guide v2.4 → v2.7

## What's New in v2.7

### 🎯 3-Level Hierarchy
Old (v2.4 and earlier):
```
Group → Address
```

New (v2.7):
```
Wallet → Account → Address
```

### 📊 Custom Ordering
Control the display order of wallets and accounts with the `order` field (lower number = appears first).

---

## How to Migrate Your Existing Data

### Option 1: Automatic (Recommended)

Your existing config will **continue to work** without changes. The old format is fully compatible:

```json
{
  "address": "bc1q...",
  "label": "My Address",
  "group": "Personal"
}
```

This will display as:
```
Personal (Wallet)
  └─ My Address
```

### Option 2: Upgrade to 3-Level Structure

**Before (v2.4):**
```json
[
  {
    "address": "bc1q...",
    "label": "Receiving",
    "group": "Trezor Default"
  },
  {
    "address": "bc1q...",
    "label": "W006",
    "group": "Trezor HODL"
  },
  {
    "address": "bc1q...",
    "label": "Personal",
    "group": "Cold Wallet HODL"
  }
]
```

**After (v2.7):**
```json
[
  {
    "address": "bc1q...",
    "label": "Receiving",
    "group": "Trezor/Default",
    "order": 1
  },
  {
    "address": "bc1q...",
    "label": "W006",
    "group": "Trezor/HODL",
    "order": 1
  },
  {
    "address": "bc1q...",
    "label": "Personal",
    "group": "Cold Wallet/HODL",
    "order": 2
  }
]
```

**Change summary:**
1. Replace space with `/` to separate Wallet and Account: `"Trezor HODL"` → `"Trezor/HODL"`
2. Add `"order": N` to control display order (optional, default is 9999)

---

## Custom Ordering Examples

### Example 1: Prioritize by Importance
```json
[
  {
    "group": "Trezor/HODL",
    "order": 1
  },
  {
    "group": "Cold Wallet/Income",
    "order": 2
  },
  {
    "group": "Watching",
    "order": 99
  }
]
```

**Display order:**
1. Trezor (order=1)
2. Cold Wallet (order=2)
3. Watching (order=99)

### Example 2: Same Wallet, Different Accounts
```json
[
  {
    "group": "Trezor/HODL",
    "order": 1
  },
  {
    "group": "Trezor/Trading",
    "order": 5
  },
  {
    "group": "Trezor/Gift",
    "order": 10
  }
]
```

**Display:** All under "Trezor" wallet, but HODL appears before Trading, Trading before Gift.

---

## Quick Migration Script

For your current data structure, here's a find-and-replace pattern:

### Trezor addresses
- Find: `"group": "Trezor Default"`
- Replace: `"group": "Trezor/Default", "order": 1`

- Find: `"group": "Trezor Alex"`
- Replace: `"group": "Trezor/Alex", "order": 1`

- Find: `"group": "Trezor Urga"`
- Replace: `"group": "Trezor/Urga", "order": 1`

- Find: `"group": "Trezor HODL"`
- Replace: `"group": "Trezor/HODL", "order": 1`

### Cold Wallet addresses
- Find: `"group": "Cold Wallet Decoy"`
- Replace: `"group": "Cold Wallet/Decoy", "order": 2`

- Find: `"group": "Cold Wallet HODL"`
- Replace: `"group": "Cold Wallet/HODL", "order": 2`

### Darwin addresses
- Find: `"group": "Darwin Mongol"`
- Replace: `"group": "Darwin/Mongol", "order": 3`

### Gift addresses
- Find: `"group": "Gift - Spend"`
- Replace: `"group": "Gift/Spend", "order": 10`

---

## Display Comparison

### Before (v2.4)
```
Cold Wallet Decoy
  0.00012340 BTC
  └─ CW Decoy  0.00012340 BTC

Cold Wallet HODL
  1.23456789 BTC
  └─ CW Personal  0.50000000 BTC
  └─ Kraken Sam W005  0.30000000 BTC
  └─ Kraken Sancho W012  0.43456789 BTC

Trezor Alex
  0.15000000 BTC
  └─ Alex  0.03000000 BTC
  └─ Alex  0.05000000 BTC
  ...
```

### After (v2.7 with migration)
```
Trezor                          [order=1, appears first]
  1.50000000 BTC
  
  Alex                          [sub-account]
    0.15000000 BTC
    └─ Alex  0.03000000 BTC
    └─ Alex  0.05000000 BTC
    
  Default                       [sub-account]
    0.80000000 BTC
    └─ Urga Zeus  0.30000000 BTC
    └─ Change  0.50000000 BTC
    
  HODL                          [sub-account]
    0.55000000 BTC
    └─ Sancho Kraken W006  0.10000000 BTC
    └─ Sancho Kraken W007  0.15000000 BTC

---

Cold Wallet                     [order=2]
  1.30000000 BTC
  
  Decoy                         [sub-account]
    0.00012340 BTC
    └─ CW Decoy  0.00012340 BTC
    
  HODL                          [sub-account]
    1.29987660 BTC
    └─ CW Personal  0.50000000 BTC
    └─ Kraken Sam W005  0.30000000 BTC
```

**Benefits:**
- Clearer hierarchy (wallet → account → address)
- Control display order (important wallets first)
- Collapsed structure (easier to scan)
- Per-account totals visible

---

## FAQ

**Q: Do I have to migrate immediately?**  
A: No. Your existing config works without changes. Migrate when you're ready.

**Q: What if I don't add `order` field?**  
A: Default is 9999. Items are then sorted alphabetically.

**Q: Can I have addresses without accounts?**  
A: Yes. Use `"group": "WalletName"` (no slash). They'll show directly under the wallet.

**Q: What happens to "Ungrouped"?**  
A: Still works. Addresses with empty `group` field appear in "Ungrouped" at the bottom.

**Q: Can I mix old and new formats?**  
A: Yes. You can have some addresses with `"Trezor"` and others with `"Trezor/HODL"`.

---

## Need Help?

Check the sample config:
```bash
cat plugins/.btcaddresses.sample.v2.7.json
```

Or open an issue on GitHub.
