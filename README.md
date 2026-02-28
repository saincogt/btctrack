# btc-track

Privately track Bitcoin address balances from the command line.

All queries are routed through **Tor** to a `.onion` endpoint — no clearnet request, no IP leakage.  
Address list is stored locally and never leaves your machine.

---

## Setup

```bash
# Install dependencies
pip install -r requirements.txt

# Start Tor (macOS)
brew install tor && brew services start tor
```

---

## Usage

```bash
# Add one or more addresses
python btc-track.py add <addr1> [addr2 ...]

# Remove an address
python btc-track.py remove <addr>

# List tracked addresses
python btc-track.py list

# Check all balances (via Tor)
python btc-track.py check

# Check without Tor (less private, for debugging)
python btc-track.py check --no-tor
```

### Example output

```
🧅 Querying via Tor (.onion) — your IP is hidden

  Address                                        Confirmed (BTC)    Unconfirmed     TXs
  ─────────────────────────────────────────────────────────────────────────────────────
  bc1q...xxxx                                         0.12345678                     12
  1A1z...xxxx                                         0.50000000   +0.01000000        5
  ─────────────────────────────────────────────────────────────────────────────────────
  TOTAL                                               0.62345678   +0.01000000
```

---

## Address storage

Addresses are saved to `~/.btc-addresses.json` (excluded from git via `.gitignore`).

---

## Privacy notes

- Default mode queries `mempool.space` via its `.onion` address over Tor — the server only sees a Tor exit node, never your real IP.
- Tor must be running locally on port `9050`.
- Never commit your address list to a public repo.
