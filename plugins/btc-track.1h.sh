#!/usr/bin/env bash
# <swiftbar.title>BTC Track</swiftbar.title>
# <swiftbar.version>1.0</swiftbar.version>
# <swiftbar.desc>Bitcoin balance tracker via Tor</swiftbar.desc>
# <swiftbar.hideAbout>true</swiftbar.hideAbout>
# <swiftbar.hideRunInTerminal>true</swiftbar.hideRunInTerminal>
# <swiftbar.hideLastUpdated>true</swiftbar.hideLastUpdated>
# <swiftbar.hideDisablePlugin>true</swiftbar.hideDisablePlugin>
# <swiftbar.hideSwiftBar>true</swiftbar.hideSwiftBar>

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PYTHON="$(command -v python3 || command -v python)"
BTC_SCRIPT="$SCRIPT_DIR/btc-track.py"

# Run check and capture output
OUTPUT="$("$PYTHON" "$BTC_SCRIPT" check 2>&1)"
EXIT_CODE=$?

# Extract total BTC from TOTAL line
TOTAL=$(echo "$OUTPUT" | awk '/^  TOTAL/ { print $2; exit }')

# ── Menu bar line (only this line appears as the icon) ──────────────────────
if [ -z "$TOTAL" ] || [ "$EXIT_CODE" -ne 0 ]; then
  echo "BTC: ?"
else
  echo "BTC ${TOTAL}"
fi

echo "---"

# ── Dropdown content ─────────────────────────────────────────────────────────
if [ -z "$TOTAL" ] || [ "$EXIT_CODE" -ne 0 ]; then
  echo "Error fetching data"
  echo "Make sure Tor is running | color=#ff6b6b"
  echo "brew services start tor | bash=/bin/bash param1=-c param2='brew services start tor' terminal=false"
else
  # Print each address row (between dividers, skip header/total)
  echo "$OUTPUT" | awk '
    /^  -{3,}/ { div++; next }
    /^  Address/ { next }
    /^  TOTAL/ { next }
    /^WARNING|^Querying/ { next }
    div == 1 && /[a-zA-Z0-9]/ {
      line = substr($0, 3)
      addr  = substr(line, 1, 45); gsub(/[[:space:]]+$/, "", addr)
      label = substr(line, 47, 20); gsub(/[[:space:]]+$/, "", label)
      btc   = substr(line, 67, 16); gsub(/[[:space:]]/, "", btc)
      if (label != "")
        printf "%s  %s BTC\n", label, btc
      else
        printf "%s  %s BTC\n", addr, btc
    }
  '
fi

echo "---"
echo "Last updated: $(date '+%H:%M') | color=#666666 size=11"
echo "Refresh | refresh=true"
echo "Open Terminal | bash=/usr/bin/osascript param1=-e param2='tell app \"Terminal\" to do script \"cd $SCRIPT_DIR && $PYTHON $BTC_SCRIPT check\"' terminal=false"
