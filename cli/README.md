# btctrack CLI

Command-line tool and local web server for tracking Bitcoin address balances privately via Tor.

## Requirements

- Python 3
- [Tor](https://www.torproject.org/)

```bash
# macOS
brew install tor && brew services start tor
```

Install Python dependencies:

```bash
pip install -r requirements.txt
```

## Setup

Copy the address file template:

```bash
cp addresses.sample.json addresses.json
```

`addresses.json` is gitignored — your addresses never leave your machine.

## CLI usage

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

# Check without Tor (faster, less private -- for debugging only)
python btctrack.py check --no-tor
```

## Web server (phone / browser)

Runs a local HTTP server on your Mac. Open it on any device on the same Wi-Fi. Queries still go through Tor — your phone's IP never touches mempool.space.

```bash
python btcserver.py              # http://0.0.0.0:8765
python btcserver.py --port 9000
python btcserver.py --refresh 10 # refresh cache every 10 min (default: 5)
python btcserver.py --no-tor     # skip Tor (less private)
```

Open `http://<your-mac-ip>:8765` on your phone.

Find your Mac IP: System Settings > Wi-Fi > Details

## Address file format

```json
[
  {
    "address": "bc1q...",
    "label": "Cold storage",
    "note": "Ledger hardware wallet"
  }
]
```

| Field | Required | Description |
|---|---|---|
| `address` | yes | Bitcoin address |
| `label` | no | Display name |
| `note` | no | Private memo (not used by SwiftBar) |

## Privacy

All requests route through Tor to a `.onion` endpoint. Query order is randomized and 0.5-2s delays are inserted between queries to prevent timing correlation. See [PRIVACY.md](../PRIVACY.md) for the full threat model.
