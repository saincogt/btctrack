#!/bin/bash
# <swiftbar.title>BTC Track</swiftbar.title>
# <swiftbar.version>1.0.0</swiftbar.version>
# <swiftbar.desc>Bitcoin address balance tracker via Tor. 3-level hierarchy with custom ordering.</swiftbar.desc>
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
# HIERARCHY
#   - 3-level structure: Wallet / Account / Address
#   - Use "group": "Wallet/Account" (e.g. "Trezor/HODL")
#   - Custom order: add "order": 1 to JSON entries (lower = higher priority)
#   - Old format "group": "Simple" still works (2-level)
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
  # Step 1: Address
  cat >"$WORK/dlg_addr.scpt" <<'APEOF'
try
  set d to display dialog "Address:" default answer "" with title "Add Address (1/3)" buttons {"Cancel", "Next ➜"} default button "Next ➜"
  return text returned of d
on error
  return ""
end try
APEOF

  ADDR=$(osascript "$WORK/dlg_addr.scpt" 2>/dev/null)
  [ -z "$ADDR" ] && exit 0

  # Step 2: Label
  cat >"$WORK/dlg_label.scpt" <<'APEOF'
try
  set d to display dialog "Label (optional):" default answer "" with title "Add Address (2/3)" buttons {"Cancel", "Next ➜"} default button "Next ➜"
  return text returned of d
on error
  return ""
end try
APEOF

  LABEL=$(osascript "$WORK/dlg_label.scpt" 2>/dev/null)

  # Step 3: Group
  cat >"$WORK/dlg_group.scpt" <<'APEOF'
try
  set d to display dialog "Group (optional):" default answer "" with title "Add Address (3/3)" buttons {"Cancel", "Done ✓"} default button "Done ✓"
  return text returned of d
on error
  return ""
end try
APEOF

  GROUP=$(osascript "$WORK/dlg_group.scpt" 2>/dev/null)

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

# ── Parse addresses from JSON (preserve original order with index) ────────────
cat >"$WORK/parse.py" <<'PYEOF'
import json, sys
data = json.load(open(sys.argv[1]))
for idx, entry in enumerate(data):
    if isinstance(entry, dict):
        addr = entry.get("address", "")
        label = entry.get("label", "") or "---"
        group = entry.get("group", "") or "---"
        order = entry.get("order", 9999)  # Default high value = low priority
        print(str(idx) + "\t" + addr + "\t" + label + "\t" + group + "\t" + str(order))
    elif isinstance(entry, str):
        print(str(idx) + "\t" + entry + "\t---\t---\t9999")
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
while IFS=$'\t' read -r idx addr label group order; do
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

  printf '%s|%s|%s|%s|%s|%s\n' "$idx" "$addr" "$label" "$group" "$order" "$SATS" >>"$WORK/results.txt"
done <"$WORK/addrs_shuffled.tsv"

# ── Compute total BTC and organize by 3-level hierarchy ──────────────────────
cat >"$WORK/organize.py" <<'PYEOF'
import sys
from collections import defaultdict

# Structure: wallets -> accounts -> addresses
wallets = defaultdict(lambda: defaultdict(list))
wallet_order = {}  # Track min order per wallet
account_order = {}  # Track min order per account
total_all = 0

for line in open(sys.argv[1]):
    parts = line.strip().split('|')
    if len(parts) == 6:
        idx, addr, label, group, order, sats = parts
        
        # Parse group: "Wallet/Account" or just "Wallet"
        if '/' in group:
            wallet, account = group.split('/', 1)
        else:
            wallet = group if group else "Ungrouped"
            account = ""
        
        # Track minimum order for sorting
        order_val = int(order)
        wallet_key = wallet
        account_key = (wallet, account)
        
        if wallet_key not in wallet_order:
            wallet_order[wallet_key] = order_val
        else:
            wallet_order[wallet_key] = min(wallet_order[wallet_key], order_val)
        
        if account_key not in account_order:
            account_order[account_key] = order_val
        else:
            account_order[account_key] = min(account_order[account_key], order_val)
        
        # Store: (original_idx, addr, label, order, sats)
        wallets[wallet][account].append((int(idx), addr, label, order_val, sats))
        
        try:
            total_all += int(sats)
        except ValueError:
            pass

# Sort wallets by order, then alphabetically (Ungrouped last)
sorted_wallets = sorted(wallets.keys(), 
                       key=lambda w: (w == "Ungrouped", wallet_order.get(w, 9999), w))

for wallet in sorted_wallets:
    accounts = wallets[wallet]
    
    # Calculate wallet total
    wallet_total = 0
    for account in accounts:
        for idx, addr, label, order, sats in accounts[account]:
            try:
                wallet_total += int(sats)
            except ValueError:
                pass
    
    # Output wallet header
    print("WALLET:" + wallet + "|" + str(wallet_total))
    
    # Sort accounts by order, then alphabetically (empty account = default, goes first)
    sorted_accounts = sorted(accounts.keys(),
                           key=lambda a: (a != "", account_order.get((wallet, a), 9999), a))
    
    for account in sorted_accounts:
        # Sort addresses by original index to maintain user's order
        addresses = sorted(accounts[account], key=lambda x: x[0])
        
        # Calculate account total
        account_total = 0
        for idx, addr, label, order, sats in addresses:
            try:
                account_total += int(sats)
            except ValueError:
                pass
        
        # Output account header (if not empty)
        if account:
            print("ACCOUNT:" + account + "|" + str(account_total))
        
        # Output addresses
        for idx, addr, label, order, sats in addresses:
            print("ADDRESS:" + addr + "|" + label + "|" + sats)

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

# ── Dropdown: organized by 3-level hierarchy (Wallet > Account > Address) ────
CURRENT_WALLET=""
CURRENT_ACCOUNT=""

while IFS= read -r line; do
  if [[ "$line" =~ ^WALLET: ]]; then
    # Parse wallet name and total: WALLET:name|total_sats
    WALLET_LINE="${line#WALLET:}"
    IFS='|' read -r CURRENT_WALLET WALLET_TOTAL_SATS <<<"$WALLET_LINE"
    
    # Separator between wallets
    [ -n "$LAST_WAS_WALLET" ] && echo "---"
    
    # Display wallet header
    WALLET_BTC=$("$PYTHON3" -c "print('{:.8f}'.format($WALLET_TOTAL_SATS/1e8))" 2>/dev/null)
    echo "${CURRENT_WALLET} | color=#5DA3FA size=15 weight=bold"
    echo "${WALLET_BTC} BTC | color=#999999 size=11"
    
    LAST_WAS_WALLET=1
    CURRENT_ACCOUNT=""
    
  elif [[ "$line" =~ ^ACCOUNT: ]]; then
    # Parse account name and total: ACCOUNT:name|total_sats
    ACCOUNT_LINE="${line#ACCOUNT:}"
    IFS='|' read -r CURRENT_ACCOUNT ACCOUNT_TOTAL_SATS <<<"$ACCOUNT_LINE"
    
    # Display account as sub-level
    ACCOUNT_BTC=$("$PYTHON3" -c "print('{:.8f}'.format($ACCOUNT_TOTAL_SATS/1e8))" 2>/dev/null)
    echo "-- ${CURRENT_ACCOUNT} | color=#7AB8FF size=13"
    echo "-- ${ACCOUNT_BTC} BTC | color=#AAAAAA size=10"
    
  elif [[ "$line" =~ ^ADDRESS: ]]; then
    # Parse address: ADDRESS:addr|label|sats
    ADDR_LINE="${line#ADDRESS:}"
    IFS='|' read -r addr label sats <<<"$ADDR_LINE"
    
    SHORT="$(echo "$addr" | awk '{print substr($0,1,8)"..."substr($0,length($0)-3,4)}')"
    DISPLAY="${label:-$SHORT}"
    
    # Determine indentation level (with or without account)
    if [ -n "$CURRENT_ACCOUNT" ]; then
      PREFIX="----"    # Wallet > Account > Address
      SUB_PREFIX="------"
    else
      PREFIX="--"      # Wallet > Address (no account)
      SUB_PREFIX="----"
    fi
    
    if [ "$sats" = "ERROR" ]; then
      echo "${PREFIX} ${DISPLAY}    ⚠ fetch error | color=#ff6b6b"
    else
      BTC="$("$PYTHON3" -c "print('{:.8f}'.format($sats/1e8))" 2>/dev/null)"
      echo "${PREFIX} ${DISPLAY}    ${BTC} BTC"
    fi
    
    # Address actions
    echo "${SUB_PREFIX} 📋 ${addr} | font=Menlo size=13 bash=/usr/bin/pbcopy param1=${addr} terminal=false"
    echo "${SUB_PREFIX} ✎ Edit Label | bash=$PLUGIN_PATH param1=--label param2=$addr terminal=false refresh=true"
    echo "${SUB_PREFIX} 📁 Edit Group | bash=$PLUGIN_PATH param1=--group param2=$addr terminal=false refresh=true"
    echo "${SUB_PREFIX} ✕ Remove      | bash=$PLUGIN_PATH param1=--remove param2=$addr terminal=false refresh=true"
    
  elif [[ "$line" =~ ^TOTAL: ]]; then
    # End of data
    :
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
