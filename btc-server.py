#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
btc-server - Local HTTP server for viewing BTC balances on phone/browser

Runs on your Mac, accessible on your local network.
Queries are made via Tor from the server -- your phone IP never touches mempool.
Results are cached; background thread refreshes every REFRESH_MINUTES.

Usage:
  python btc-server.py              # starts on http://0.0.0.0:8765
  python btc-server.py --port 9000
  python btc-server.py --no-tor     # skip Tor (less private)
  python btc-server.py --refresh 10 # refresh every 10 minutes (default: 5)

Then open http://<your-mac-ip>:8765 on your phone (same WiFi).
Find your Mac IP: System Settings > Wi-Fi > Details
"""

from __future__ import print_function

import json, os, sys, argparse, time, threading

SCRIPT_DIR     = os.path.dirname(os.path.abspath(__file__))
ADDRESSES_FILE = os.path.join(SCRIPT_DIR, "addresses.json")

TOR_PROXY = {
    "http":  "socks5h://127.0.0.1:9050",
    "https": "socks5h://127.0.0.1:9050",
}
MEMPOOL_ONION    = "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion"
MEMPOOL_CLEARNET = "https://mempool.space"

# ── shared cache ──────────────────────────────────────────────────────────────
_cache_lock    = threading.Lock()
_cached_html   = None        # last rendered page
_cache_updated = None        # epoch time of last successful refresh
_cache_status  = "Starting..." # human-readable status shown on page


def load_addresses():
    if not os.path.exists(ADDRESSES_FILE):
        return []
    with open(ADDRESSES_FILE) as f:
        raw = json.load(f)
    result = []
    for entry in raw:
        if not isinstance(entry, dict):
            result.append({"address": entry, "label": "", "note": ""})
        else:
            entry.setdefault("label", "")
            entry.setdefault("note", "")
            result.append(entry)
    return result


def fetch_balance(addr, proxies, base_url):
    import requests
    url  = "{}/api/address/{}".format(base_url, addr)
    resp = requests.get(url, proxies=proxies, timeout=30)
    resp.raise_for_status()
    d = resp.json()
    c = d.get("chain_stats",   {})
    m = d.get("mempool_stats", {})
    confirmed   = c.get("funded_txo_sum", 0) - c.get("spent_txo_sum", 0)
    unconfirmed = m.get("funded_txo_sum", 0) - m.get("spent_txo_sum", 0)
    return confirmed, unconfirmed, c.get("tx_count", 0)


# ── HTML ──────────────────────────────────────────────────────────────────────

PAGE = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
<title>BTC Track</title>
<style>
*{{box-sizing:border-box;margin:0;padding:0}}
body{{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:#0a0a0a;color:#e8e8e8;padding:20px 16px 48px;max-width:600px;margin:0 auto}}
h1{{font-size:12px;color:#444;letter-spacing:.1em;text-transform:uppercase;margin-bottom:18px}}
.badge{{display:inline-block;background:#12121e;border:1px solid #2a2a44;border-radius:6px;padding:3px 9px;font-size:11px;color:#7070cc;margin-bottom:14px}}
.total{{background:#1a1a1a;border-radius:16px;padding:24px 20px;margin-bottom:14px;text-align:center}}
.tlbl{{font-size:12px;color:#555;margin-bottom:6px}}
.tbtc{{font-size:40px;font-weight:700;color:#f7931a;letter-spacing:-.5px}}
.tunit{{font-size:15px;color:#777;margin-left:5px}}
.tunconf{{font-size:12px;color:#f7931a;opacity:.65;margin-top:5px}}
.cards{{display:flex;flex-direction:column;gap:10px}}
.card{{background:#1a1a1a;border-radius:12px;padding:15px 16px;display:flex;justify-content:space-between;align-items:center;gap:12px}}
.left{{overflow:hidden;min-width:0}}
.lbl{{font-size:15px;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-bottom:3px}}
.hash{{font-size:10px;color:#444;font-family:monospace;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}}
.note{{font-size:11px;color:#4a4a4a;margin-top:2px}}
.right{{text-align:right;flex-shrink:0}}
.btc{{font-size:15px;font-weight:600}}
.txs{{font-size:10px;color:#444;margin-top:2px}}
.runconf{{font-size:10px;color:#f7931a;opacity:.65}}
.err{{color:#e05555}}
.status{{text-align:center;margin-top:22px;font-size:12px;color:#444}}
.refresh{{display:block;width:100%;margin-top:14px;background:#161616;border:1px solid #222;color:#666;border-radius:10px;padding:13px;font-size:14px;text-align:center;text-decoration:none}}
</style>
</head>
<body>
<h1>BTC Track</h1>
{badge}
<div class="total">
  <div class="tlbl">Total Balance</div>
  <div><span class="tbtc">{total_btc}</span><span class="tunit">BTC</span></div>
  {total_unconf}
</div>
<div class="cards">{cards}</div>
<a class="refresh" href="/">Refresh</a>
<div class="status">{status}</div>
</body></html>"""

CARD = """<div class="card">
  <div class="left">
    <div class="lbl">{label}</div>
    <div class="hash">{address}</div>{note_html}
  </div>
  <div class="right">
    <div class="btc {err}">{btc}</div>{txs_html}{unconf_html}
  </div>
</div>"""

LOADING_PAGE = """<!DOCTYPE html><html><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta http-equiv="refresh" content="5">
<title>BTC Track - Loading</title>
<style>body{{background:#0a0a0a;color:#888;font-family:-apple-system,sans-serif;
display:flex;align-items:center;justify-content:center;height:100vh;margin:0;flex-direction:column;gap:12px}}
.spinner{{width:32px;height:32px;border:3px solid #222;border-top-color:#f7931a;border-radius:50%;animation:spin .8s linear infinite}}
@keyframes spin{{to{{transform:rotate(360deg)}}}}</style></head>
<body><div class="spinner"></div><div>Fetching balances...</div>
<div style="font-size:12px;color:#444">Page will auto-refresh</div></body></html>"""


def render(entries, results, use_tor, status_msg):
    total_c = total_u = 0
    cards = []
    for e, res in zip(entries, results):
        addr  = e["address"]
        label = e.get("label") or (addr[:10] + "..." + addr[-6:])
        note  = e.get("note", "")
        note_html = '\n    <div class="note">{}</div>'.format(note) if note else ""
        if res is None:
            btc_s = "?"
            txs_html = unconf_html = ""
            err = "err"
        else:
            c, u, txs = res
            total_c += c
            total_u += u
            btc_s = "{:.8f}".format(c / 1e8)
            txs_html   = '\n    <div class="txs">{} txs</div>'.format(txs)
            unconf_html = '\n    <div class="runconf">{:+.8f} unconf</div>'.format(u / 1e8) if u else ""
            err = ""
        cards.append(CARD.format(
            label=label, address=addr, note_html=note_html,
            btc=btc_s, txs_html=txs_html, unconf_html=unconf_html, err=err,
        ))

    badge        = '<span class="badge">Tor enabled</span>' if use_tor else ""
    total_btc    = "{:.8f}".format(total_c / 1e8)
    total_unconf = '<div class="tunconf">{:+.8f} unconfirmed</div>'.format(total_u / 1e8) if total_u else ""
    return PAGE.format(
        badge=badge, total_btc=total_btc, total_unconf=total_unconf,
        cards="\n".join(cards), status=status_msg,
    )


# ── background refresh ────────────────────────────────────────────────────────

def refresh_loop(use_tor, interval_minutes):
    global _cached_html, _cache_updated, _cache_status
    proxies  = TOR_PROXY if use_tor else {}
    base_url = MEMPOOL_ONION if use_tor else MEMPOOL_CLEARNET

    while True:
        entries = load_addresses()
        results = []
        _cache_status = "Fetching {} address(es)...".format(len(entries))

        for i, e in enumerate(entries):
            _cache_status = "Fetching {}/{}...".format(i + 1, len(entries))
            try:
                results.append(fetch_balance(e["address"], proxies, base_url))
            except Exception as ex:
                print("[WARN] {}: {}".format(e["address"][:20], ex))
                results.append(None)
            time.sleep(0.4)  # gentle rate-limit buffer between requests

        _cache_updated = time.time()
        updated_str    = "Updated {}  (auto-refresh every {} min)".format(
            time.strftime("%H:%M:%S"), interval_minutes
        )
        _cache_status  = updated_str

        html = render(entries, results, use_tor, updated_str)
        with _cache_lock:
            _cached_html = html.encode("utf-8")

        print("[{}] Cache refreshed ({} addresses)".format(
            time.strftime("%H:%M:%S"), len(entries)
        ))
        time.sleep(interval_minutes * 60)


# ── HTTP handler ──────────────────────────────────────────────────────────────

def make_handler():
    if sys.version_info[0] >= 3:
        from http.server import BaseHTTPRequestHandler
    else:
        from BaseHTTPServer import BaseHTTPRequestHandler

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            with _cache_lock:
                html = _cached_html

            if html is None:
                data = LOADING_PAGE.encode("utf-8")
            else:
                data = html

            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

        def log_message(self, fmt, *args):
            print("[{}] {}".format(time.strftime("%H:%M:%S"), fmt % args))

    return Handler


# ── main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Local web server for BTC balances")
    parser.add_argument("--port",    type=int,   default=8765)
    parser.add_argument("--refresh", type=float, default=5,
                        help="Cache refresh interval in minutes (default: 5)")
    parser.add_argument("--no-tor",  action="store_true", help="Skip Tor proxy")
    args = parser.parse_args()

    use_tor = not args.no_tor

    if sys.version_info[0] >= 3:
        from http.server import HTTPServer
    else:
        from BaseHTTPServer import HTTPServer

    import socket
    try:
        local_ip = socket.gethostbyname(socket.gethostname())
    except Exception:
        local_ip = "?.?.?.?"

    # Start background refresh thread
    t = threading.Thread(target=refresh_loop, args=(use_tor, args.refresh), daemon=True)
    t.start()

    server = HTTPServer(("0.0.0.0", args.port), make_handler())

    print("BTC Track server running")
    print("  Mac:    http://localhost:{}".format(args.port))
    print("  Phone:  http://{}:{} (same WiFi)".format(local_ip, args.port))
    print("  Tor:    {}".format("enabled" if use_tor else "DISABLED"))
    print("  Refresh every {} min".format(args.refresh))
    print("\nCtrl+C to stop\n")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()
