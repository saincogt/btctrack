# Changelog

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
