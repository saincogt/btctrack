# btc-track

Privately track Bitcoin address balances — CLI, Mac menu bar, and phone browser.

All queries are routed through **Tor** to a `.onion` endpoint.  
Address list is stored locally and never leaves your machine.

---

## Setup

```bash
pip install -r requirements.txt

# Start Tor (macOS)
brew install tor && brew services start tor
```

---

## CLI usage

```bash
# Add address (with optional label and note)
python btc-track.py add <addr> -l "Cold storage" -n "Ledger"

# Remove an address
python btc-track.py remove <addr>

# Set/update label or note for an existing address
python btc-track.py label <addr> "New label"
python btc-track.py note  <addr> "New note"

# List tracked addresses
python btc-track.py list

# Check all balances (via Tor)
python btc-track.py check

# Check without Tor (less private, for debugging)
python btc-track.py check --no-tor
```

### Example output

```
Querying via Tor (.onion) -- your IP is hidden

  Address                                       Label                 Confirmed (BTC)    Unconfirmed     TXs
  -----------------------------------------------------------------------------------------------------------
  bc1q...xxxx                                   Cold storage               0.12345678                     12
  1A1z...xxxx                                   Exchange                   0.50000000   +0.01000000        5
  -----------------------------------------------------------------------------------------------------------
  TOTAL                                                                    0.62345678   +0.01000000
```

---

## Mac menu bar (xbar / SwiftBar)

Shows total BTC in the menu bar, per-address breakdown on click. Auto-refreshes.

1. Install [xbar](https://xbarapp.com) or [SwiftBar](https://swiftbar.app)
2. Symlink the plugin into the plugins directory:
   ```bash
   # xbar
   ln -s "$(pwd)/btc-track.1h.sh" ~/Library/Application\ Support/xbar/plugins/

   # SwiftBar
   ln -s "$(pwd)/btc-track.1h.sh" ~/Library/Application\ Support/SwiftBar/plugins/
   ```
3. The filename controls refresh interval — rename to change it:
   - `btc-track.30m.sh` → every 30 minutes
   - `btc-track.1h.sh`  → every hour

---

## Phone / browser (local network)

Runs a local web server on your Mac. Your phone accesses it over Wi-Fi.  
Queries still go through Tor — your phone IP never touches mempool.space.

```bash
python btc-server.py              # http://0.0.0.0:8765
python btc-server.py --port 9000  # custom port
python btc-server.py --refresh 10 # refresh cache every 10 min (default: 5)
```

Open `http://<your-mac-ip>:8765` on your phone.  
*(Find Mac IP: System Settings → Wi-Fi → Details)*

---

## Address storage

Addresses are saved to `addresses.json` (excluded from git).  
Copy the template to get started:

```bash
cp addresses.sample.json addresses.json
```

Format — each entry supports `address`, `label`, and `note`:

```json
[
  {
    "address": "bc1q...",
    "label": "Cold storage",
    "note": "Ledger hardware wallet"
  }
]
```

---

## Privacy notes

- Default mode uses Tor + `.onion` endpoint — server sees only a Tor exit node.
- Tor must be running on port `9050`.
- `addresses.json` is gitignored — never commit it to a public repo.
