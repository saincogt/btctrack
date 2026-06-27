# btctrack SwiftBar plugin

macOS menu bar plugin that displays a BTC icon and auto-refreshes balances every hour. Balance is hidden by default for privacy -- click to expand per-address details.

## Requirements

- macOS
- [SwiftBar](https://swiftbar.app) (free, open source)
- [Tor](https://www.torproject.org/)

```bash
brew install tor && brew services start tor
```

## Setup

1. Install [SwiftBar](https://swiftbar.app)
2. Open SwiftBar > **Change Plugin Folder...** > select this `swiftbar/` directory
3. SwiftBar will load `btctrack.1h.sh` automatically
4. Click the BTC icon in the menu bar > **+ Add Address** to add your first address

Config is stored in `swiftbar/.btcaddresses.json` (gitignored). Use **Edit Config** in the menu to open it in your editor.

## Change refresh interval

Rename the script file to change how often it refreshes:

| Filename | Interval |
|---|---|
| `btctrack.5m.sh` | 5 minutes |
| `btctrack.30m.sh` | 30 minutes |
| `btctrack.1h.sh` | 1 hour (default) |
| `btctrack.6h.sh` | 6 hours |
| `btctrack.12h.sh` | 12 hours |

```bash
# Example: change to 30 minutes
mv btctrack.1h.sh btctrack.30m.sh
```

Then click the BTC icon > **Refresh** (or wait for SwiftBar to pick up the rename).

## Address file format

`.btcaddresses.json` is created automatically when you add the first address via the menu. You can also edit it directly:

```json
[
  {
    "address": "bc1q...",
    "label": "Cold Storage",
    "group": "Trezor/Personal",
    "order": 1
  }
]
```

| Field | Required | Description |
|---|---|---|
| `address` | yes | Bitcoin address |
| `label` | no | Display name (truncated address shown if empty) |
| `group` | no | Hierarchy path, slash-separated: `"Wallet"` or `"Wallet/Account"` |
| `order` | no | Display priority (lower = first, default 9999) |

### Group hierarchy example

```
Trezor  (order=1)
  3.50 BTC
  Personal
    0.80 BTC
    Cold Storage
  HODL
    2.70 BTC
    Long-term
---
Cold Wallet  (order=2)
  1.20 BTC
  Income
    Mining
```

## Per-address menu actions

| Action | Description |
|---|---|
| Copy address | Copy raw address to clipboard |
| Edit label | Rename the display label |
| Edit group | Change the group path |
| Remove | Delete from tracking |

## Privacy

All queries are routed through Tor to a `.onion` endpoint. Your IP is never exposed. Query order is randomized on every refresh. See [PRIVACY.md](../PRIVACY.md) for details.
