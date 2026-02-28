#!/usr/bin/env bash
# <swiftbar.title>BTC Track</swiftbar.title>
# <swiftbar.version>2.0</swiftbar.version>
# <swiftbar.desc>Bitcoin address balance tracker via Tor. No pip installs needed.</swiftbar.desc>
# <swiftbar.hideAbout>true</swiftbar.hideAbout>
# <swiftbar.hideRunInTerminal>false</swiftbar.hideRunInTerminal>
# <swiftbar.hideLastUpdated>true</swiftbar.hideLastUpdated>
# <swiftbar.hideDisablePlugin>false</swiftbar.hideDisablePlugin>
# <swiftbar.hideSwiftBar>false</swiftbar.hideSwiftBar>
#
# SETUP
#   1. Place btc-addresses.json in the same folder as this plugin:
#        cp btc-addresses.sample.json btc-addresses.json
#        # then edit btc-addresses.json with your addresses
#   2. Install & start Tor:
#        brew install tor && brew services start tor
#
# btc-addresses.json format:
#   [{"address":"bc1q...","label":"My Wallet"}, {"address":"bc1q..."}]
#
# Dependencies: curl (macOS built-in), python3 (macOS built-in) — no pip needed.

PLUGIN_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG="$PLUGIN_DIR/btc-addresses.json"
TOR_PROXY="socks5h://127.0.0.1:9050"
ONION_API="http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/api/address"
CLEAR_API="https://mempool.space/api/address"

# Prefer Homebrew python3; fall back to system python3
PYTHON3="/opt/homebrew/bin/python3"
[ -x "$PYTHON3" ] || PYTHON3="/usr/bin/python3"

# ── Setup check ───────────────────────────────────────────────────────────────
if [ ! -f "$CONFIG" ]; then
  echo "BTC ⚙"
  echo "---"
  echo "Setup needed | color=#ff6b6b"
  echo "Copy btc-addresses.sample.json → btc-addresses.json | color=#888888 size=11"
  echo "---"
  echo "Refresh | refresh=true"
  exit 0
fi

# ── Parse addresses from JSON ─────────────────────────────────────────────────
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

"$PYTHON3" - "$CONFIG" >"$WORK/addrs.tsv" 2>/dev/null <<'PYEOF'
import json, sys
data = json.load(open(sys.argv[1]))
for entry in data:
    if isinstance(entry, dict):
        print(entry.get("address", "") + "\t" + entry.get("label", ""))
    elif isinstance(entry, str):
        print(entry + "\t")
PYEOF

if [ ! -s "$WORK/addrs.tsv" ]; then
  echo "BTC ?"
  echo "---"
  echo "No addresses in btc-addresses.json | color=#ff6b6b"
  exit 0
fi

# ── Fetch balance for each address via Tor (fallback: clearnet) ───────────────
while IFS=$'\t' read -r addr label; do
  [ -z "$addr" ] && continue
  RESP="$WORK/${addr}.json"

  curl -sf --max-time 30 --proxy "$TOR_PROXY" "$ONION_API/$addr" -o "$RESP" 2>/dev/null
  if [ $? -ne 0 ] || [ ! -s "$RESP" ]; then
    curl -sf --max-time 15 "$CLEAR_API/$addr" -o "$RESP" 2>/dev/null
  fi

  if [ -s "$RESP" ]; then
    SATS="$("$PYTHON3" -c "
import json
d = json.load(open('$RESP'))
c, m = d['chain_stats'], d['mempool_stats']
print(c['funded_txo_sum'] - c['spent_txo_sum'] + m['funded_txo_sum'] - m['spent_txo_sum'])
" 2>/dev/null)"
    [ -z "$SATS" ] && SATS="ERROR"
  else
    SATS="ERROR"
  fi

  printf '%s|%s|%s\n' "$addr" "$label" "$SATS" >>"$WORK/results.txt"
done <"$WORK/addrs.tsv"

# ── Compute total BTC ─────────────────────────────────────────────────────────
TOTAL_BTC="$("$PYTHON3" - "$WORK/results.txt" <<'PYEOF'
import sys
total = 0
for line in open(sys.argv[1]):
    parts = line.strip().split('|')
    if len(parts) == 3:
        try: total += int(parts[2])
        except ValueError: pass
print('{:.8f}'.format(total / 1e8))
PYEOF
)"

HAS_ERROR="$(grep -c '|ERROR$' "$WORK/results.txt" 2>/dev/null)"
HAS_ERROR="${HAS_ERROR:-0}"

# ── Menu bar line (first line = icon text) ────────────────────────────────────
if [ -z "$TOTAL_BTC" ]; then
  echo "BTC ?"
else
  echo "BTC ${TOTAL_BTC}"
fi

echo "---"

# ── Dropdown rows ─────────────────────────────────────────────────────────────
if [ -s "$WORK/results.txt" ]; then
  while IFS='|' read -r addr label sats; do
    SHORT="$(echo "$addr" | awk '{print substr($0,1,8)"..."substr($0,length($0)-3,4)}')"
    DISPLAY="${label:-$SHORT}"
    if [ "$sats" = "ERROR" ]; then
      echo "${DISPLAY}    fetch error | color=#ff6b6b tooltip=${addr}"
    else
      BTC="$("$PYTHON3" -c "print('{:.8f}'.format($sats/1e8))" 2>/dev/null)"
      echo "${DISPLAY}    ${BTC} BTC | tooltip=${addr}"
    fi
  done <"$WORK/results.txt"
  echo "---"
  echo "Total: ${TOTAL_BTC} BTC | color=#f7931a"
fi

echo "---"
if [ "$HAS_ERROR" -gt 0 ]; then
  echo "Tor may not be running | color=#ff6b6b size=11"
  echo "Start Tor | bash=/opt/homebrew/bin/brew param1=services param2=start param3=tor terminal=false refresh=true"
fi
echo "Last updated: $(date '+%H:%M') | color=#666666 size=11"
echo "Refresh | refresh=true"
