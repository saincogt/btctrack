# Privacy & Security

## How btctrack protects you

btctrack uses multiple layers of protection to prevent third parties from correlating your queries.

### 1. IP hiding (Tor routing)
All requests are routed through the Tor network to a `.onion` endpoint. The mempool.space server only sees a Tor exit node — your real IP is never exposed. Timestamps and User-Agent headers are decoupled from your identity.

### 2. Address correlation prevention
Even with a hidden IP, querying multiple addresses in rapid sequence could allow a server to infer they belong to the same person through timing analysis.

btctrack mitigates this with:
- **Randomized query order** — address list is shuffled before every refresh
- **Random delays** — 0.5–2s random pause between each query
- **Local storage** — address list never leaves your device

### 3. Balance visibility
The menu bar displays only a ₿ icon. No balance is shown on screen, protecting you from shoulder-surfing.

### 4. Local-only data
All addresses, labels, and groups are stored in a local JSON file. It is gitignored by default. There is no cloud sync, no telemetry, and no third-party service involved beyond the balance lookup itself.

---

## Threat model

| Threat | Risk | Mitigation | Status |
|--------|------|------------|--------|
| IP tracking | Server logs your IP | Tor + .onion endpoint | ✅ Mitigated |
| Timing correlation | Burst queries link addresses to one user | Random order + random delays | ✅ Mitigated |
| Query fingerprinting | Fixed order creates a recognizable pattern | Shuffled on every refresh | ✅ Mitigated |
| Address list leak | JSON committed to a public repo | gitignored by default | ✅ Mitigated |
| Local access | Physical access to your device | System-level encryption (FileVault) | ⚠️ User responsibility |
| On-chain analysis | Transaction graph links your addresses | Out of scope for btctrack | ❌ Not addressed |

---

## Residual risks

### Long-term pattern analysis
If you track a large number of addresses (50+), a server may — over months — statistically identify that "this set of addresses is always queried together", even with randomization.

**Mitigation:** split addresses across multiple tracking instances, or increase the refresh interval.

### Single Tor circuit
All queries within one refresh cycle may share the same Tor circuit. The server cannot see your IP, but it can observe that requests arrive on the same circuit.

**Advanced mitigation (optional):**
- Send a `NEWNYM` signal to Tor after each query to force circuit rotation
- Use multiple Tor instances on different SOCKS ports

### On-chain privacy
btctrack does **not** protect on-chain privacy. If your addresses are linked by transactions (shared inputs, change outputs), blockchain analytics firms can still cluster them regardless of how you query.

**Mitigation:** use CoinJoin (Wasabi Wallet, Samourai Whirlpool), avoid UTXO merging across sources, consider Lightning Network.

---

## Best practices

**Do:**
- Use system disk encryption (macOS FileVault)
- Keep a reasonable refresh interval (1–6 hours)
- Verify `addresses.json` and `plugins/.btcaddresses.json` are gitignored before pushing

**Don't:**
- Commit address files to GitHub
- Store your address list in cloud notes (Notion, Evernote, iCloud)
- Set a very short refresh interval (e.g. every 5 minutes) — this increases correlation risk

---

## When btctrack is not enough

If your threat model includes government-level surveillance, institutional-scale holdings, or requirements for complete on-chain anonymity, btctrack is not the right tool. Consider:

1. **Run your own Bitcoin full node** with Electrum Personal Server — all queries resolved locally, zero network leakage
2. **Hardware wallet watch-only mode** — Ledger Live / Trezor Suite connected to your own node
3. **Air-gapped device** — store your address list offline and query manually

---

btctrack is designed for privacy-conscious individuals who want convenient balance monitoring without exposing their IP or address list. It is not designed for adversarial threat models.
