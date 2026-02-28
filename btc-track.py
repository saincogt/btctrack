#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
btc-track - Track Bitcoin address balances privately via Tor

Usage:
  python btc-track.py add <address> [address2 ...]    # Add addresses
  python btc-track.py remove <address> [address2 ...]  # Remove addresses
  python btc-track.py list                             # List tracked addresses
  python btc-track.py check                            # Check all balances
  python btc-track.py check --no-tor                   # Skip Tor (less private)
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
        return json.load(f)


def save_addresses(addresses):
    with open(ADDRESSES_FILE, "w") as f:
        json.dump(addresses, f, indent=2)


def cmd_add(args):
    addresses = load_addresses()
    for addr in args.addresses:
        if addr in addresses:
            print("  already tracked: {}".format(addr))
        else:
            addresses.append(addr)
            print("  added: {}".format(addr))
    save_addresses(addresses)


def cmd_remove(args):
    addresses = load_addresses()
    for addr in args.addresses:
        if addr not in addresses:
            print("  not found: {}".format(addr))
        else:
            addresses.remove(addr)
            print("  removed: {}".format(addr))
    save_addresses(addresses)


def cmd_list(args):
    addresses = load_addresses()
    if not addresses:
        print("No addresses tracked. Use: python btc-track.py add <address>")
        return
    for i, addr in enumerate(addresses, 1):
        print("  {:>3}.  {}".format(i, addr))
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

    col_addr = 45
    header = "  {:<{w}} {:>16} {:>14}  {:>6}".format(
        "Address", "Confirmed (BTC)", "Unconfirmed", "TXs", w=col_addr
    )
    divider = "  " + "-" * (col_addr + 42)
    print(header)
    print(divider)

    total_confirmed = 0
    total_unconfirmed = 0
    errors = 0

    for addr in addresses:
        try:
            confirmed, unconfirmed, tx_count = fetch_balance(addr, proxies, base_url)
            total_confirmed += confirmed
            total_unconfirmed += unconfirmed
            btc = confirmed / 1e8
            ubtc = unconfirmed / 1e8
            ustr = "{:>+.8f}".format(ubtc) if unconfirmed != 0 else ""
            print("  {:<{w}} {:>16.8f} {:>14}  {:>6}".format(
                addr, btc, ustr, tx_count, w=col_addr
            ))
        except Exception as e:
            print("  {:<{w}} {:>16}  ({})".format(addr, "ERROR", e, w=col_addr))
            errors += 1

    print(divider)
    total_btc = total_confirmed / 1e8
    total_ubtc = total_unconfirmed / 1e8
    ustr = "{:>+.8f}".format(total_ubtc) if total_unconfirmed != 0 else ""
    print("  {:<{w}} {:>16.8f} {:>14}".format("TOTAL", total_btc, ustr, w=col_addr))

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

    p_add = sub.add_parser("add", help="Add one or more addresses to track")
    p_add.add_argument("addresses", nargs="+")

    p_rm = sub.add_parser("remove", help="Remove one or more tracked addresses")
    p_rm.add_argument("addresses", nargs="+")

    sub.add_parser("list", help="List all tracked addresses")

    p_check = sub.add_parser("check", help="Check balances of all tracked addresses")
    p_check.add_argument("--no-tor", action="store_true", help="Skip Tor proxy (less private)")

    args = parser.parse_args()
    if not args.command:
        parser.print_help()
        sys.exit(1)

    {"add": cmd_add, "remove": cmd_remove, "list": cmd_list, "check": cmd_check}[args.command](args)


if __name__ == "__main__":
    main()

