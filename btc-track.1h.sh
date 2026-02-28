#!/usr/bin/env bash
# <xbar.title>BTC Track</xbar.title>
# <xbar.version>1.0</xbar.version>
# <xbar.desc>Show Bitcoin address balances via Tor</xbar.desc>
# <xbar.refreshOnOpen>true</xbar.refreshOnOpen>
#
# Filename controls refresh interval, e.g.:
#   btc-track.30m.sh  -> every 30 minutes
#   btc-track.1h.sh   -> every hour
#
# Install:
#   1. Install xbar: https://xbarapp.com  (or SwiftBar: https://swiftbar.app)
#   2. Copy/symlink this file into the xbar plugins directory
#   3. Make sure Tor is running: brew install tor && brew services start tor

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PYTHON="$(command -v python3 || command -v python)"
BTC_SCRIPT="$SCRIPT_DIR/btc-track.py"

# ── helpers ──────────────────────────────────────────────────────────────────

run_check() {
  "$PYTHON" "$BTC_SCRIPT" check 2>&1
}

# ── parse output ─────────────────────────────────────────────────────────────

OUTPUT="$(run_check)"

# Extract TOTAL line
TOTAL=$(echo "$OUTPUT" | awk '/^  TOTAL/ {print $2}')

if [ -z "$TOTAL" ]; then
  echo "BTC ?"
  echo "---"
  echo "Error fetching data"
  echo "$OUTPUT" | head -5
  exit 0
fi

# ── menu bar line ─────────────────────────────────────────────────────────────
echo "${TOTAL} BTC"
echo "---"

# ── dropdown: per-address rows ────────────────────────────────────────────────
# Skip header, divider, total; print address rows
echo "$OUTPUT" | awk '
  /^  -{3,}/ { divider++; next }
  /^  Address/ { next }
  /^  TOTAL/ { next }
  /^WARNING|^Querying/ { next }
  divider == 1 && /\S/ {
    addr  = substr($0, 3, 45); gsub(/[[:space:]]+$/, "", addr)
    rest  = substr($0, 48)
    # extract label (chars 46-65), btc (66-81), txs (last field)
    label = substr($0, 49, 20); gsub(/[[:space:]]+$/, "", label)
    btc   = substr($0, 69, 16); gsub(/[[:space:]]/, "", btc)
    if (label != "") {
      printf "  %-20s  %s BTC\n", label, btc
    } else {
      printf "  %s  %s BTC\n", addr, btc
    }
  }
'

echo "---"
UPDATED="$(date '+%H:%M')"
echo "Last updated: $UPDATED | color=#999999"
echo "Refresh | refresh=true"
echo "Open terminal | bash=/bin/bash param1=-c param2=\"cd '$SCRIPT_DIR' && '$PYTHON' '$BTC_SCRIPT' check\" terminal=true"
