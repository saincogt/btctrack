# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2026-03-01

### Features
- **Bitcoin Balance Tracking**: Monitor multiple addresses via Tor-routed queries
- **3-Level Hierarchy**: Organize addresses as Wallet → Account → Address
- **Custom Ordering**: Control display priority with numeric `order` field
- **SwiftBar Integration**: macOS menu bar plugin with auto-refresh
- **Privacy Protection**: Randomized query order + random delays (0.5-2s)
- **Tor Routing**: All queries via `.onion` endpoint, IP hidden
- **CLI Tools**: Add, remove, label, and check addresses from command line
- **Web Server**: Access from mobile devices on local network
- **Cross-Platform**: Intel Mac and Apple Silicon support

### SwiftBar Plugin
- Menu bar ₿ icon with expandable address list
- Per-wallet and per-account balance totals
- Configurable refresh intervals (5m, 30m, 1h, 6h, 12h)
- Address actions: copy, edit label, edit group, remove
- Smart error diagnostics for Tor connectivity

### Privacy
- IP masking via Tor + .onion endpoints
- Query order randomized on every refresh
- Random delays between queries to prevent timing correlation
- Local-only storage, no cloud sync or telemetry
- Menu bar shows icon only, balance hidden from screen

### Documentation
- Comprehensive README with setup instructions
- Privacy threat model (PRIVACY.md)
- Address file format examples
