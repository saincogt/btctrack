#!/usr/bin/env bash
# <swiftbar.title>BTC Track</swiftbar.title>
# <swiftbar.version>1.1</swiftbar.version>
# <swiftbar.desc>Bitcoin balance tracker via Tor</swiftbar.desc>
# <swiftbar.hideAbout>true</swiftbar.hideAbout>
# <swiftbar.hideRunInTerminal>true</swiftbar.hideRunInTerminal>
# <swiftbar.hideLastUpdated>true</swiftbar.hideLastUpdated>
# <swiftbar.hideDisablePlugin>false</swiftbar.hideDisablePlugin>
# <swiftbar.hideSwiftBar>false</swiftbar.hideSwiftBar>

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PYTHON="/opt/homebrew/bin/python3"
[ -x "$PYTHON" ] || PYTHON="$(command -v python3 || command -v python)"
BTC_SCRIPT="$SCRIPT_DIR/btc-track.py"

# Truncate address: first 8 chars + ... + last 4 chars
shorten_addr() {
  echo "$1" | awk '{ print substr($0,1,8) "..." substr($0,length($0)-3,4) }'
}

# Run check and capture output
OUTPUT="$("$PYTHON" "$BTC_SCRIPT" check 2>&1)"
EXIT_CODE=$?

# Extract total BTC from TOTAL line
TOTAL=$(echo "$OUTPUT" | awk '/^  TOTAL/ { print $2; exit }')

# ── Menu bar line ────────────────────────────────────────────────────────────
if [ -z "$TOTAL" ] || [ "$EXIT_CODE" -ne 0 ]; then
  echo "BTC ?"
else
  echo "BTC ${TOTAL}"
fi

echo "---"

# ── Dropdown content ─────────────────────────────────────────────────────────
if [ -z "$TOTAL" ] || [ "$EXIT_CODE" -ne 0 ]; then
  echo "Error fetching data | color=#ff6b6b"
  ERRMSG=$(echo "$OUTPUT" | grep -v "^$" | head -1)
  echo "${ERRMSG} | color=#888888 size=11"
  echo "---"
  echo "Start Tor | bash=/opt/homebrew/bin/brew param1=services param2=start param3=tor terminal=false refresh=true"
else
  # Parse each address row, print shortened with tooltip for full address
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
      short = substr(addr,1,8) "..." substr(addr,length(addr)-3,4)
      display = (label != "") ? label : short
      printf "%s    %s BTC | tooltip=%s\n", display, btc, addr
    }
  '
  echo "---"
  echo "Total: ${TOTAL} BTC | color=#f7931a"
fi

echo "---"
echo "Last updated: $(date '+%H:%M') | color=#666666 size=11"
echo "Refresh | refresh=true"
echo "Open in Terminal | bash=/usr/bin/osascript param1=-e param2='tell app \"Terminal\" to do script \"cd $SCRIPT_DIR && $PYTHON $BTC_SCRIPT check\"' terminal=false"
