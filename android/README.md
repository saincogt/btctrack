# btctrack Android

Native Android app for tracking Bitcoin address balances. All queries route through Tor via a configurable SOCKS5 proxy. Supports biometric auth, address grouping, background refresh, and JSON import/export.

## Requirements

- Android Studio (Hedgehog or newer recommended)
- Android SDK 31+ (Android 12)
- A running Tor SOCKS5 proxy reachable from the device
  - On device: [Orbot](https://guardianproject.info/apps/org.torproject.android/) (recommended)
  - On LAN: `brew install tor && brew services start tor` on your Mac, then point the app at your Mac's LAN IP

## Build

### Android Studio

1. Open Android Studio
2. File > Open > select the `android/` directory
3. Wait for Gradle sync to finish
4. Run > Run 'app' (or press Shift+F10)

### Command line

```bash
cd android

# Debug APK
./gradlew assembleDebug

# Install directly to connected device/emulator
./gradlew installDebug

# Release APK (unsigned)
./gradlew assembleRelease
```

Output APK location: `app/build/outputs/apk/debug/app-debug.apk`

## Tor setup (Orbot)

The simplest setup is [Orbot](https://guardianproject.info/apps/org.torproject.android/) on the same device:

1. Install Orbot from F-Droid or the Play Store
2. Start Orbot and enable VPN mode, or note the SOCKS5 port (default: 9050)
3. In btctrack: Settings > Proxy > `127.0.0.1:9050`

To use a Tor instance running on your Mac (same WiFi):

1. Start Tor: `brew services start tor`
2. In btctrack: Settings > Proxy > `<mac-lan-ip>:9050`
   - Find your Mac IP: System Settings > Wi-Fi > Details

## Features

- Track multiple Bitcoin addresses with labels and custom groups
- Hierarchical group tree (slash-separated paths, e.g. `Trezor/Personal`)
- Balance visible only after biometric/PIN unlock
- Background refresh (WorkManager, configurable interval)
- Import/export addresses as JSON
- Tor-only mode: all queries use `.onion` endpoint, no clearnet fallback
- Light / Dark / System theme

## Project structure

```
android/
  app/src/main/java/com/zeal/btctrack/
    data/
      local/        # Room database, DataStore settings
      remote/       # Esplora API client (Tor-only)
    domain/
      model/        # AppSettings, TrackedAddress, etc.
      usecase/      # RefreshTrackedAddressesUseCase, etc.
    ui/
      screens/      # Compose screens
      theme/        # Color, Type, Theme
      navigation/   # NavHost, Routes
    background/     # WorkManager refresh scheduler
```

## Privacy

All network requests go through the configured SOCKS5 proxy to a `.onion` endpoint. No clearnet fallback. Timing jitter (random delays) and per-burst randomization are applied before each refresh cycle. See [PRIVACY.md](../PRIVACY.md) for the full threat model.
