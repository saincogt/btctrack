# btctrack

Privately track Bitcoin address balances. All queries route through Tor to a `.onion` endpoint -- your IP is never exposed to any third party. Address data is stored locally and never transmitted.

## Components

| Component | Platform | Description |
|---|---|---|
| [cli/](cli/) | macOS / Linux | Command-line tool + local web server for phone/browser access |
| [swiftbar/](swiftbar/) | macOS | Menu bar plugin (SwiftBar) with per-address balance and auto-refresh |
| [android/](android/) | Android 12+ | Native Android app with biometric auth, group tree, and background refresh |

Each component is independent -- use one, two, or all three. They do not share state.

## Privacy

- IP hidden via Tor + `.onion` endpoint
- Query order randomized on every refresh
- Random delays between queries (timing correlation prevention)
- All data stored locally -- never transmitted
- No analytics, no telemetry, no accounts

See [PRIVACY.md](PRIVACY.md) for the full threat model.

## Quick start

**CLI:**
```bash
cd cli
pip install -r requirements.txt
cp addresses.sample.json addresses.json
python btctrack.py add <address> -l "My wallet"
python btctrack.py check
```

**SwiftBar:**
1. Install [SwiftBar](https://swiftbar.app)
2. Set plugin folder to `swiftbar/`
3. Click the BTC icon in the menu bar

**Android:**
```bash
cd android
./gradlew installDebug
```
Then configure your Tor proxy in Settings.

See each component's README for full setup and usage details.
