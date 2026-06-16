# BTC Track Android status

## Current scope
- Watch-only address tracking
- Local Room/DataStore persistence
- Tor-only Esplora access via SOCKS5
- Shuffle + jitter refresh orchestration
- Biometric gating for sensitive reveal actions
- WorkManager periodic background refresh
- Import/export compatibility with CLI JSON and SwiftBar JSON

## Verification commands
```bash
cd /Users/zeal/Documents/study/projects/btctrack/android-app
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## Manual device checks
- Import a small CLI JSON payload
- Import a SwiftBar JSON payload
- Confirm Dashboard shows tracked count and Tor status
- Confirm Reveal balance requires biometric when enabled
- Confirm Grouped view hides full address until gated reveal
- Confirm export screen produces CLI JSON and SwiftBar JSON text
- Confirm periodic refresh is scheduled after app start and settings save

## Notes
- Tor-only enforcement stays at the network boundary.
- Refresh order and jitter are injected for testability.
- Background refresh uses WorkManager with network and battery-not-low constraints.
