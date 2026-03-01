# btctrack

Privately track Bitcoin address balances — CLI, Mac menu bar, and phone browser.

All queries are routed through **Tor** to a `.onion` endpoint. Your IP is never exposed to any third-party service. Address data is stored locally and never leaves your machine.

---

## Requirements

- Python 3
- [Tor](https://www.torproject.org/) (macOS: `brew install tor && brew services start tor`)

```bash
pip install -r requirements.txt
```

---

## CLI

```bash
# Add an address (label and note are optional)
python btctrack.py add <address> -l "Cold storage" -n "Ledger"

# Remove one or more addresses
python btctrack.py remove <address>

# List all tracked addresses
python btctrack.py list

# Update label or note
python btctrack.py label <address> "New label"
python btctrack.py note  <address> "New note"

# Check all balances (via Tor)
python btctrack.py check

# Check without Tor (faster, less private — for debugging only)
python btctrack.py check --no-tor
```

Addresses are stored in `addresses.json` (gitignored). Copy the template to get started:

```bash
cp addresses.sample.json addresses.json
```

---

## Mac menu bar (SwiftBar)

Displays a ₿ icon in the menu bar. Balance is intentionally hidden for privacy — click to expand per-address details. Auto-refreshes every hour.

**Setup:**

1. Install [SwiftBar](https://swiftbar.app)
2. Open SwiftBar → **Change Plugin Folder…** → select the `plugins/` directory
3. Click **＋ Add Address** in the menu to add your first address

The plugin stores its config in `plugins/.btcaddresses.json` (hidden file, so SwiftBar does not treat it as a plugin). Use **✎ Edit Config** in the menu to open it in TextEdit, VS Code, Cursor, or Finder.

**Rename the file to change the refresh interval:**

| Filename | Refresh |
|---|---|
| `btctrack.5m.sh` | every 5 minutes |
| `btctrack.30m.sh` | every 30 minutes |
| `btctrack.1h.sh` | every hour (default) |

**Per-address menu actions:** 📋 Copy address · ✎ Edit label · 📁 Edit group · ✕ Remove

---

## Phone / browser

Runs a local web server on your Mac, accessible from any device on the same Wi-Fi. Queries still go through Tor — your phone's IP never touches mempool.space.

```bash
python btcserver.py              # http://0.0.0.0:8765
python btcserver.py --port 9000
python btcserver.py --refresh 10 # refresh cache every 10 min (default: 5)
python btcserver.py --no-tor     # skip Tor (less private)
```

Open `http://<your-mac-ip>:8765` on your phone.
*(Find your Mac IP: System Settings → Wi-Fi → Details)*

---

## Address file format

### CLI — `addresses.json`

```json
[
  {
    "address": "bc1q...",
    "label": "Cold storage",
    "note": "Ledger hardware wallet"
  }
]
```

### SwiftBar plugin — `plugins/.btcaddresses.json`

```json
[
  {
    "address": "bc1q...",
    "label": "Cold Storage",
    "group": "Personal"
  }
]
```

`group` organizes addresses into sections in the dropdown. Any string is valid (e.g. `"Personal"`, `"Business"`, `"Income"`).

---

## Privacy

See [PRIVACY.md](PRIVACY.md) for a full threat model and analysis.

**At a glance:**
- ✅ IP hidden via Tor + `.onion` endpoint
- ✅ Query order randomized on every refresh
- ✅ Random 0.5–2s delays between queries (timing correlation prevention)
- ✅ All data stored locally — never transmitted
- ✅ Menu bar shows icon only — no balance visible on screen
