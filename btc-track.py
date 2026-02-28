#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
btc-track - Track Bitcoin address balances privately via Tor

Usage:
  python btc-track.py add <address> [-l LABEL] [-n NOTE]  # Add address
  python btc-track.py remove <address> [address2 ...]     # Remove addresses
  python btc-track.py list                                 # List tracked addresses
  python btc-track.py label <address> <label>             # Set/update label
  python btc-track.py note <address> <note>               # Set/update note
  python btc-track.py check                               # Check all balances
  python btc-track.py check --no-tor                      # Skip Tor (less private)

addresses.json format:
  [
    {
      "address": "bc1q...",
      "label": "Cold storage",
      "note": "Hardware wallet - Ledger"
    }
  ]
"""

from __future__ import print_function

import json
import sys
import os
import argparse

ADDRESSES_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "addresses.json")

TOR_PROXY = {
    "http": "socks5h://127.0.0.1:9050",
    "https": "socks5h://127.0.0.1:9050",
}

# .onion endpoints (Tor only) — no clearnet request ever leaves your machine
MEMPOOL_ONION = "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion"
MEMPOOL_CLEARNET = "https://mempool.space"


def load_addresses():
    if not os.path.exists(ADDRESSES_FILE):
        return []
    with open(ADDRESSES_FILE) as f:
        raw = json.load(f)
    # Backward compat: plain string list -> object list (handles str and Python 2 unicode)
    result = []
    for entry in raw:
        if not isinstance(entry, dict):
            result.append({"address": entry, "label": "", "note": ""})
        else:
            entry.setdefault("label", "")
            entry.setdefault("note", "")
            result.append(entry)
    return result


def save_addresses(addresses):
    with open(ADDRESSES_FILE, "w") as f:
        json.dump(addresses, f, indent=2)


def find_entry(addresses, addr):
    for entry in addresses:
        if entry["address"] == addr:
            return entry
    return None


def cmd_add(args):
    addresses = load_addresses()
    if find_entry(addresses, args.address):
        print("  already tracked: {}".format(args.address))
        return
    entry = {"address": args.address, "label": args.label or "", "note": args.note or ""}
    addresses.append(entry)
    save_addresses(addresses)
    print("  added: {}".format(args.address))
    if entry["label"]:
        print("  label: {}".format(entry["label"]))
    if entry["note"]:
        print("  note:  {}".format(entry["note"]))


def cmd_remove(args):
    addresses = load_addresses()
    for addr in args.addresses:
        entry = find_entry(addresses, addr)
        if not entry:
            print("  not found: {}".format(addr))
        else:
            addresses.remove(entry)
            print("  removed: {}".format(addr))
    save_addresses(addresses)


def cmd_label(args):
    addresses = load_addresses()
    entry = find_entry(addresses, args.address)
    if not entry:
        print("  not found: {}".format(args.address))
        return
    entry["label"] = args.label
    save_addresses(addresses)
    print("  updated label for {}: {}".format(args.address, args.label))


def cmd_note(args):
    addresses = load_addresses()
    entry = find_entry(addresses, args.address)
    if not entry:
        print("  not found: {}".format(args.address))
        return
    entry["note"] = args.note
    save_addresses(addresses)
    print("  updated note for {}: {}".format(args.address, args.note))


def cmd_list(args):
    addresses = load_addresses()
    if not addresses:
        print("No addresses tracked. Use: python btc-track.py add <address>")
        return
    for i, entry in enumerate(addresses, 1):
        label = "  [{}]".format(entry["label"]) if entry["label"] else ""
        note  = "  -- {}".format(entry["note"]) if entry["note"] else ""
        print("  {:>3}.  {}{}{}".format(i, entry["address"], label, note))
    print("\n  Total: {} address(es)".format(len(addresses)))


def fetch_balance(addr, proxies, base_url):
    try:
        import requests
    except ImportError:
        print("Missing dependency. Run:  pip install requests PySocks")
        sys.exit(1)

    url = "{}/api/address/{}".format(base_url, addr)
    resp = requests.get(url, proxies=proxies, timeout=30)
    resp.raise_for_status()
    data = resp.json()

    chain = data.get("chain_stats", {})
    mempool = data.get("mempool_stats", {})

    confirmed = chain.get("funded_txo_sum", 0) - chain.get("spent_txo_sum", 0)
    unconfirmed = mempool.get("funded_txo_sum", 0) - mempool.get("spent_txo_sum", 0)
    tx_count = chain.get("tx_count", 0)

    return confirmed, unconfirmed, tx_count


def cmd_check(args):
    addresses = load_addresses()
    if not addresses:
        print("No addresses tracked. Use: python btc-track.py add <address>")
        return

    use_tor = not args.no_tor
    proxies = TOR_PROXY if use_tor else {}
    base_url = MEMPOOL_ONION if use_tor else MEMPOOL_CLEARNET

    if use_tor:
        print("Querying via Tor (.onion) -- your IP is hidden\n")
    else:
        print("WARNING: Querying without Tor -- your IP is visible to mempool.space\n")

    col_addr  = 45
    col_label = 20
    header = "  {:<{wa}} {:<{wl}} {:>16} {:>14}  {:>6}".format(
        "Address", "Label", "Confirmed (BTC)", "Unconfirmed", "TXs",
        wa=col_addr, wl=col_label
    )
    divider = "  " + "-" * (col_addr + col_label + 44)
    print(header)
    print(divider)

    total_confirmed = 0
    total_unconfirmed = 0
    errors = 0

    for entry in addresses:
        addr  = entry["address"]
        label = entry["label"][:col_label] if entry["label"] else ""
        try:
            confirmed, unconfirmed, tx_count = fetch_balance(addr, proxies, base_url)
            total_confirmed += confirmed
            total_unconfirmed += unconfirmed
            btc  = confirmed / 1e8
            ubtc = unconfirmed / 1e8
            ustr = "{:>+.8f}".format(ubtc) if unconfirmed != 0 else ""
            print("  {:<{wa}} {:<{wl}} {:>16.8f} {:>14}  {:>6}".format(
                addr, label, btc, ustr, tx_count, wa=col_addr, wl=col_label
            ))
        except Exception as e:
            print("  {:<{wa}} {:<{wl}} {:>16}  ({})".format(
                addr, label, "ERROR", e, wa=col_addr, wl=col_label
            ))
            errors += 1

    print(divider)
    total_btc  = total_confirmed / 1e8
    total_ubtc = total_unconfirmed / 1e8
    ustr = "{:>+.8f}".format(total_ubtc) if total_unconfirmed != 0 else ""
    print("  {:<{wa}} {:<{wl}} {:>16.8f} {:>14}".format(
        "TOTAL", "", total_btc, ustr, wa=col_addr, wl=col_label
    ))

    if errors:
        print("\n  WARNING: {} address(es) failed. "
              "Tor may not be running (brew install tor && brew services start tor)".format(errors))


def main():
    parser = argparse.ArgumentParser(
        description="Track Bitcoin address balances privately via Tor",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    sub = parser.add_subparsers(dest="command")

    p_add = sub.add_parser("add", help="Add an address to track")
    p_add.add_argument("address")
    p_add.add_argument("-l", "--label", default="", help="Short label (e.g. 'Cold storage')")
    p_add.add_argument("-n", "--note",  default="", help="Free-text note")

    p_rm = sub.add_parser("remove", help="Remove one or more tracked addresses")
    p_rm.add_argument("addresses", nargs="+")

    p_lbl = sub.add_parser("label", help="Set or update the label for an address")
    p_lbl.add_argument("address")
    p_lbl.add_argument("label")

    p_note = sub.add_parser("note", help="Set or update the note for an address")
    p_note.add_argument("address")
    p_note.add_argument("note")

    sub.add_parser("list", help="List all tracked addresses")

    p_check = sub.add_parser("check", help="Check balances of all tracked addresses")
    p_check.add_argument("--no-tor", action="store_true", help="Skip Tor proxy (less private)")

    args = parser.parse_args()
    if not args.command:
        parser.print_help()
        sys.exit(1)

    {
        "add": cmd_add, "remove": cmd_remove,
        "label": cmd_label, "note": cmd_note,
        "list": cmd_list, "check": cmd_check,
    }[args.command](args)


if __name__ == "__main__":
    main()


