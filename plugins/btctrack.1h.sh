#!/bin/bash
# <swiftbar.title>BTC Track</swiftbar.title>
# <swiftbar.version>2.5</swiftbar.version>
# <swiftbar.desc>Bitcoin address balance tracker via Tor. Privacy-focused with randomized queries.</swiftbar.desc>
# <swiftbar.hideAbout>true</swiftbar.hideAbout>
# <swiftbar.hideRunInTerminal>false</swiftbar.hideRunInTerminal>
# <swiftbar.hideLastUpdated>true</swiftbar.hideLastUpdated>
# <swiftbar.hideDisablePlugin>false</swiftbar.hideDisablePlugin>
# <swiftbar.hideSwiftBar>false</swiftbar.hideSwiftBar>
#
# SETUP
#   1. Install & start Tor:  brew install tor && brew services start tor
#   2. Click the menu bar icon → "＋ Add Address" to add your first address
#
# PRIVACY FEATURES
#   - All queries via Tor .onion endpoint (IP hidden)
#   - Random query order (prevents pattern recognition)
#   - Random delays 0.5-2s between queries (prevents timing correlation)
#   - Address list stored locally only (never transmitted)
#
# Dependencies: curl (macOS built-in), python3 (macOS built-in) — no pip needed.

PLUGIN_DIR="$(cd "$(dirname "$0")" && pwd)"
PLUGIN_PATH="$PLUGIN_DIR/btctrack.1h.sh"
CONFIG="$PLUGIN_DIR/.btcaddresses.json"
TOR_PROXY="socks5h://127.0.0.1:9050"
ONION_API="http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/api/address"
CLEAR_API="https://mempool.space/api/address"

PYTHON3="/opt/homebrew/bin/python3"  # Apple Silicon
[ -x "$PYTHON3" ] || PYTHON3="/usr/local/bin/python3"  # Intel Mac
[ -x "$PYTHON3" ] || PYTHON3="/usr/bin/python3"  # System fallback

# Workspace — created early so action handlers can use it too
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# NOTE: All heredocs are at the TOP LEVEL (never inside $(...)) to avoid a
# bash 3.2 bug where heredocs inside $() cause the script to re-execute.
# Python logic is written to temp files first, then executed normally.

# ── Action handlers (invoked by menu buttons) ─────────────────────────────────

if [ "$1" = "--add" ]; then
  # Single dialog with multiple fields
  cat >"$WORK/dlg_add.scpt" <<'APEOF'
try
  set dialogText to "Enter one item per line:" & return & return & "1. Address (required)" & return & "2. Label (optional - e.g. Cold Storage)" & return & "3. Group (optional - e.g. Personal, Business)"
  set d to display dialog dialogText default answer "" with title "BTC Track - Add Address" buttons {"Cancel", "Add"} default button "Add"
  return text returned of d
on error
  return ""
end try
APEOF

  INPUT=$(osascript "$WORK/dlg_add.scpt" 2>/dev/null)
  [ -z "$INPUT" ] && exit 0

  # Parse multi-line input
  cat >"$WORK/parse_input.py" <<'PYEOF'
import sys
lines = sys.argv[1].strip().split('\n')
addr = lines[0].strip() if len(lines) > 0 else ""
label = lines[1].strip() if len(lines) > 1 else ""
group = lines[2].strip() if len(lines) > 2 else ""
print(addr + "\t" + label + "\t" + group)
PYEOF

  PARSED=$("$PYTHON3" "$WORK/parse_input.py" "$INPUT")
  IFS=$'\t' read -r ADDR LABEL GROUP <<<"$PARSED"

  [ -z "$ADDR" ] && exit 0

  cat >"$WORK/add.py" <<'PYEOF'
import json, sys
config_file, addr, label, group = sys.argv[1], sys.argv[2].strip(), sys.argv[3].strip(), sys.argv[4].strip()
if not addr:
    sys.exit(1)
try:
    data = json.load(open(config_file))
except:
    data = []
if any((e.get("address") if isinstance(e, dict) else e) == addr for e in data):
    sys.exit(0)
entry = {"address": addr}
if label:
    entry["label"] = label
if group:
    entry["group"] = group
data.append(entry)
with open(config_file, "w") as f:
    json.dump(data, f, indent=2)
PYEOF

  "$PYTHON3" "$WORK/add.py" "$CONFIG" "$ADDR" "$LABEL" "$GROUP"
  exit 0
fi

if [ "$1" = "--remove" ]; then
  cat >"$WORK/remove.py" <<'PYEOF'
import json, sys
config_file, addr = sys.argv[1], sys.argv[2]
data = json.load(open(config_file))
data = [e for e in data if (e.get("address") if isinstance(e, dict) else e) != addr]
with open(config_file, "w") as f:
    json.dump(data, f, indent=2)
PYEOF

  "$PYTHON3" "$WORK/remove.py" "$CONFIG" "$2"
  exit 0
fi

if [ "$1" = "--label" ]; then
  ADDR="$2"
  # Write dialog script with address embedded
  printf 'try\n  set d to display dialog "New label for %s:" default answer "" with title "BTC Track - Edit Label" buttons {"Cancel", "Save"} default button "Save"\n  return text returned of d\non error\n  return ""\nend try\n' "$ADDR" >"$WORK/dlg_edit.scpt"

  NEW_LABEL=$(osascript "$WORK/dlg_edit.scpt" 2>/dev/null)
  [ -z "$NEW_LABEL" ] && exit 0

  cat >"$WORK/relabel.py" <<'PYEOF'
import json, sys
config_file, addr, label = sys.argv[1], sys.argv[2], sys.argv[3]
data = json.load(open(config_file))
for i, e in enumerate(data):
    a = e.get("address") if isinstance(e, dict) else e
    if a == addr:
        if isinstance(e, dict):
            data[i]["label"] = label
        else:
            data[i] = {"address": addr, "label": label}
with open(config_file, "w") as f:
    json.dump(data, f, indent=2)
PYEOF

  "$PYTHON3" "$WORK/relabel.py" "$CONFIG" "$ADDR" "$NEW_LABEL"
  exit 0
fi

if [ "$1" = "--group" ]; then
  ADDR="$2"
  printf 'try\n  set d to display dialog "New group for %s:" default answer "" with title "BTC Track - Edit Group" buttons {"Cancel", "Save"} default button "Save"\n  return text returned of d\non error\n  return ""\nend try\n' "$ADDR" >"$WORK/dlg_grp.scpt"

  NEW_GROUP=$(osascript "$WORK/dlg_grp.scpt" 2>/dev/null)
  [ -z "$NEW_GROUP" ] && exit 0

  cat >"$WORK/regroup.py" <<'PYEOF'
import json, sys
config_file, addr, group = sys.argv[1], sys.argv[2], sys.argv[3]
data = json.load(open(config_file))
for i, e in enumerate(data):
    a = e.get("address") if isinstance(e, dict) else e
    if a == addr:
        if isinstance(e, dict):
            data[i]["group"] = group
        else:
            data[i] = {"address": addr, "group": group}
with open(config_file, "w") as f:
    json.dump(data, f, indent=2)
PYEOF

  "$PYTHON3" "$WORK/regroup.py" "$CONFIG" "$ADDR" "$NEW_GROUP"
  exit 0
fi

# ── No addresses yet ──────────────────────────────────────────────────────────
if [ ! -f "$CONFIG" ]; then
  echo "| sfimage=bitcoinsign.circle color=#f7931a"
  echo "---"
  echo "No addresses yet"
  echo "＋ Add Address | bash=$PLUGIN_PATH param1=--add terminal=false refresh=true"
  echo "---"
  echo "Refresh | refresh=true"
  exit 0
fi

# ── Parse addresses from JSON ─────────────────────────────────────────────────
cat >"$WORK/parse.py" <<'PYEOF'
import json, sys
data = json.load(open(sys.argv[1]))
for entry in data:
    if isinstance(entry, dict):
        addr = entry.get("address", "")
        label = entry.get("label", "") or "---"
        group = entry.get("group", "") or "---"
        print(addr + "\t" + label + "\t" + group)
    elif isinstance(entry, str):
        print(entry + "\t---\t---")
PYEOF

"$PYTHON3" "$WORK/parse.py" "$CONFIG" >"$WORK/addrs.tsv" 2>/dev/null

if [ ! -s "$WORK/addrs.tsv" ]; then
  echo "| sfimage=bitcoinsign.circle color=#f7931a"
  echo "---"
  echo "Config file error or empty | color=#ff6b6b"
  echo "＋ Add Address | bash=$PLUGIN_PATH param1=--add terminal=false refresh=true"
  echo "✎ Edit Config"
  echo "-- TextEdit | bash=/usr/bin/open param1=-e param2=$CONFIG terminal=false"
  exit 0
fi

# ── Shuffle addresses for privacy (avoid timing correlation attacks) ─────────
cat >"$WORK/shuffle.py" <<'PYEOF'
import sys, random, time
# Seed with current time in microseconds for better randomness
random.seed(time.time())
lines = open(sys.argv[1]).readlines()
random.shuffle(lines)
for line in lines:
    print(line.rstrip())
PYEOF

"$PYTHON3" "$WORK/shuffle.py" "$WORK/addrs.tsv" >"$WORK/addrs_shuffled.tsv"

# ── Fetch balance for each address via Tor (fallback: clearnet) ───────────────
# Privacy: random query order + random delays to prevent timing correlation
while IFS=$'\t' read -r addr label group; do
  [ -z "$addr" ] && continue
  # Convert placeholders back to empty strings
  [ "$label" = "---" ] && label=""
  [ "$group" = "---" ] && group=""

  # Random delay 0.5-2 seconds (prevents timing correlation)
  DELAY=$(awk 'BEGIN{srand(); printf "%.2f", 0.5 + rand() * 1.5}')
  sleep "$DELAY"

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

  printf '%s|%s|%s|%s\n' "$addr" "$label" "$group" "$SATS" >>"$WORK/results.txt"
done <"$WORK/addrs_shuffled.tsv"

# ── Compute total BTC and organize by groups ─────────────────────────────────
cat >"$WORK/organize.py" <<'PYEOF'
import sys
from collections import defaultdict

groups = defaultdict(list)
total_all = 0

for line in open(sys.argv[1]):
    parts = line.strip().split('|')
    if len(parts) == 4:
        addr, label, group, sats = parts
        group_name = group if group else "Ungrouped"
        groups[group_name].append((addr, label, sats))
        try:
            total_all += int(sats)
        except ValueError:
            pass

# Output: groups sorted, with Ungrouped last
for grp in sorted(groups.keys(), key=lambda x: (x == "Ungrouped", x)):
    # Calculate group total
    group_total = 0
    for addr, label, sats in groups[grp]:
        try:
            group_total += int(sats)
        except ValueError:
            pass
    
    # Output: GROUP:name|total_sats
    print("GROUP:" + grp + "|" + str(group_total))
    for addr, label, sats in groups[grp]:
        print(addr + "|" + label + "|" + sats)

print("TOTAL:" + str(total_all))
PYEOF

"$PYTHON3" "$WORK/organize.py" "$WORK/results.txt" >"$WORK/organized.txt"

TOTAL_BTC=$(grep '^TOTAL:' "$WORK/organized.txt" | cut -d: -f2)
TOTAL_BTC=$("$PYTHON3" -c "print('{:.8f}'.format($TOTAL_BTC/1e8))" 2>/dev/null)

HAS_ERROR="$(grep -c '|ERROR$' "$WORK/results.txt" 2>/dev/null)"
HAS_ERROR="${HAS_ERROR:-0}"

# ── Menu bar line ─────────────────────────────────────────────────────────────
echo "| sfimage=bitcoinsign.circle.fill color=#f7931a"
echo "---"

# ── Dropdown: organized by groups ─────────────────────────────────────────────
CURRENT_GROUP=""
GROUP_TOTAL=0

while IFS= read -r line; do
  if [[ "$line" =~ ^GROUP: ]]; then
    # Output previous group subtotal at bottom
    if [ -n "$CURRENT_GROUP" ] && [ "$GROUP_TOTAL" -gt 0 ]; then
      echo "---"
    fi

    # Parse group name and total: GROUP:name|total_sats
    GROUP_LINE="${line#GROUP:}"
    IFS='|' read -r CURRENT_GROUP GROUP_TOTAL_SATS <<<"$GROUP_LINE"
    GROUP_TOTAL=0

    # Display group header: name on first line, total on second line (both at top level)
    GROUP_HEADER_BTC=$("$PYTHON3" -c "print('{:.8f}'.format($GROUP_TOTAL_SATS/1e8))" 2>/dev/null)

    # Always show group header (including Ungrouped)
    echo "${CURRENT_GROUP} | color=#5DA3FA size=14"
    echo "${GROUP_HEADER_BTC} BTC | color=#999999 size=11"
  elif [[ "$line" =~ ^TOTAL: ]]; then
    # Final group separator
    if [ -n "$CURRENT_GROUP" ] && [ "$GROUP_TOTAL" -gt 0 ]; then
      echo "---"
    fi
  else
    IFS='|' read -r addr label sats <<<"$line"
    SHORT="$(echo "$addr" | awk '{print substr($0,1,8)"..."substr($0,length($0)-3,4)}')"
    DISPLAY="${label:-$SHORT}"

    if [ "$sats" = "ERROR" ]; then
      echo "-- ${DISPLAY}    ⚠ fetch error | color=#ff6b6b"
    else
      BTC="$("$PYTHON3" -c "print('{:.8f}'.format($sats/1e8))" 2>/dev/null)"
      echo "-- ${DISPLAY}    ${BTC} BTC"
      GROUP_TOTAL=$((GROUP_TOTAL + sats))
    fi
    echo "---- 📋 ${addr} | font=Menlo size=13 bash=/usr/bin/pbcopy param1=${addr} terminal=false"
    echo "---- ✎ Edit Label | bash=$PLUGIN_PATH param1=--label param2=$addr terminal=false refresh=true"
    echo "---- 📁 Edit Group | bash=$PLUGIN_PATH param1=--group param2=$addr terminal=false refresh=true"
    echo "---- ✕ Remove      | bash=$PLUGIN_PATH param1=--remove param2=$addr terminal=false refresh=true"
  fi
done <"$WORK/organized.txt"

echo "---"
echo "Total: ${TOTAL_BTC:-?} BTC | color=#f7931a"
echo "---"
echo "＋ Add Address | bash=$PLUGIN_PATH param1=--add terminal=false refresh=true"
echo "✎ Edit Config"
echo "-- TextEdit          | bash=/usr/bin/open param1=-e param2=$CONFIG terminal=false"
echo "-- VS Code           | bash=/usr/bin/open param1=-a param2=Visual Studio Code param3=$CONFIG terminal=false"
echo "-- Cursor            | bash=/usr/bin/open param1=-a param2=Cursor param3=$CONFIG terminal=false"
echo "-- Reveal in Finder  | bash=/usr/bin/open param1=-R param2=$CONFIG terminal=false"
echo "---"
if [ "$HAS_ERROR" -gt 0 ]; then
  # Check if Tor is installed
  if ! command -v tor >/dev/null 2>&1; then
    echo "⚠ Tor not installed | color=#ff6b6b size=11"
    echo "-- Install: brew install tor | terminal=false"
    echo "-- Guide | href=https://formulae.brew.sh/formula/tor"
  else
    # Tor installed but may not be running
    if ! pgrep -x tor >/dev/null 2>&1; then
      echo "⚠ Tor not running | color=#ff6b6b size=11"
      # Detect Homebrew location
      BREW="/opt/homebrew/bin/brew"
      [ -x "$BREW" ] || BREW="/usr/local/bin/brew"
      if [ -x "$BREW" ]; then
        echo "Start Tor | bash=$BREW param1=services param2=start param3=tor terminal=false refresh=true"
      else
        echo "Start Tor: brew services start tor | terminal=false"
      fi
    else
      echo "⚠ Network error | color=#ff6b6b size=11"
      echo "-- Check Tor proxy: 127.0.0.1:9050 | terminal=false"
    fi
  fi
fi
echo "Last updated: $(date '+%H:%M') | color=#666666 size=11"
echo "Refresh | refresh=true"
