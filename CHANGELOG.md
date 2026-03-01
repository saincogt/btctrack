# Changelog

## v2.7 (2026-03-01)

### 🎉 Major Features
- **3-Level Hierarchy**: Organize addresses as Wallet → Account → Address
  - Use `"group": "Wallet/Account"` format (e.g. `"Trezor/HODL"`)
  - Old 2-level format `"group": "Personal"` still works (backward compatible)
  - Visual hierarchy with proper indentation in menu dropdown
  
- **Custom Ordering**: Control display order with `order` field
  - Add `"order": 1` to JSON entries (lower number = higher priority)
  - Wallets and accounts sort by order first, then alphabetically
  - Default order is 9999 (appears last)
  - Example: `order=1` for important wallets, `order=99` for watching addresses

### 🔧 Improvements
- Per-wallet totals displayed prominently with bold styling
- Per-account subtotals shown under each account  
- Better visual separation between wallets (separator lines)
- Addresses maintain JSON file order within groups
- Proper indentation for 3-level structure (-- for 2-level, ---- for 3-level)

### 📚 Documentation
- Created [GUIDE_v2.7.md](GUIDE_v2.7.md) - Complete usage guide (8KB)
- Created [MIGRATION_v2.7.md](MIGRATION_v2.7.md) - Upgrade instructions (5KB)
- Created [UPGRADE_v2.5_说明.md](UPGRADE_v2.5_说明.md) - Chinese quick start guide
- Added sample config `.btcaddresses.sample.v2.7.json`
- Updated README with hierarchy examples

### ⚙️ Technical
- Refactored organize.py to support 3-level structure with nested defaultdict
- Parse script now extracts `order` field from JSON (default 9999)
- Display logic rewritten to handle WALLET/ACCOUNT/ADDRESS markers
- Wallet and account sorting by minimum order value within group
- Full backward compatibility with v2.4-v2.6 configs
- Code expanded from 398 to 471 lines

---

## v2.6 (2026-03-01)

### ✨ Improved
- **Consistent display order**: Addresses now display in JSON file order within each group
  - Query order is still randomized for privacy protection
  - Display order is restored to match JSON file sequence
  - Users can reorder addresses by editing the JSON file manually
  - Each refresh now shows predictable, consistent ordering

---

## v2.5.1 (2026-03-01)

### 🔧 Fixed
- **Address input dialogs**: Reverted to 3-step dialog flow (macOS limitation)
  - macOS `display dialog` doesn't support multi-line input - pressing Enter submits the form
  - Improved 3-step flow with progress indicators: "Add Address (1/3)", "(2/3)", "(3/3)"
  - Better button labels: "Next ➜" and "Done ✓" for clearer navigation
  - Each step shows only one field for simpler, faster input

---

## v2.5 (2026-03-01) - REVERTED

~~Attempted single-dialog input - didn't work due to macOS dialog limitations~~

---

## v2.4 (2026-03-01)

### 🔧 Fixed
- **Cross-platform compatibility**: Added Intel Mac support for Python and Homebrew paths
  - Python detection now tries: Apple Silicon (`/opt/homebrew/bin`) → Intel (`/usr/local/bin`) → System (`/usr/bin`)
  - Brew detection for Tor startup button supports both Apple Silicon and Intel Macs
  - Changed shebang to `/bin/bash` for broader compatibility (was `/opt/homebrew/bin/bash`)

- **Improved error messages**: Smarter diagnostics when queries fail
  - Distinguishes between "Tor not installed", "Tor not running", and "Network error"
  - Provides actionable guidance for each error state
  - Added links to installation guides

- **Better config error handling**: Clear messaging when JSON is malformed or empty
  - Shows "Config file error or empty" instead of generic "No addresses"
  - Provides quick access to edit config directly from error state

- **Enhanced UI polish**:
  - Updated group header color to `#5DA3FA` for better visibility in both light and dark modes
  - Added warning emoji (⚠) to error states for better visual scanning

### 📚 Documentation
- **Clarified config file differences** between CLI and SwiftBar plugin
  - CLI supports `note` field, plugin does not
  - Plugin supports `group` field, CLI does not
  - Added explicit field descriptions to avoid confusion

- **Expanded refresh interval examples** in README
  - Added 6h and 12h options
  - Clarified that users can rename the file to any valid SwiftBar interval

- **Improved privacy documentation**:
  - Added risk assessment context for single Tor circuit usage
  - Expanded long-term pattern analysis section with practical guidance
  - Clarified when mitigations are necessary vs. optional

### 🔐 Security
- No changes to privacy/security features (Tor routing, randomization, delays remain unchanged)

---

## v2.3 (Previous)
- Added address grouping functionality
- Implemented random query order and delays for timing correlation prevention
- Created comprehensive privacy documentation (PRIVACY.md)
- Two-line group headers with per-group totals

## v2.2 and earlier
- See git history
