# Changelog

Notable changes to Rish Deskdroid are recorded here.

## 1.1.0 — 2026-10-11

### Changed
- The app is now called **RishDesk**, with your RD artwork as the adaptive icon (black on cream, plus a monochrome layer for themed icons). The application ID is unchanged, so this installs over the previous build.
- APK files are named `RishDesk-debug.apk` and `RishDesk-1.1.0-preview.apk`.

### Added
- Home screen: configurable grid (default 5 x 4), drag-and-drop shortcuts, folders (create, rename, add, remove), custom labels, resizable widgets that follow each widget's own size limits, edit mode.
- App drawer: search with clear button and empty state, All / Favorites / Recent filters, sort (A-Z, Z-A, newest, recent), favorites, hide and restore apps, configurable grid and icon size, long-press menu (Add to Home, dock, favorite, hide, App info, Uninstall with confirmation).
- Dock: move, remove, alignment, size, optional recent-apps panel (launcher-tracked, no extra permission), OTG tile that opens Android's USB/storage screen.
- Settings: accent colour, animations, date in status bar, Android status bar toggle, wallpaper fit / position / dim / blur, hidden apps, reset appearance / desktop / dock / everything (with confirmation), shortcuts for Wi-Fi, Bluetooth, Display, Storage, Files, Developer options, default-launcher chooser.
- Keyboard: Ctrl, Alt or Win + D / F / / / , / H / 1-9, Esc, arrow keys and Tab focus, Enter to open. Enabled by default.
- Optional "Open apps in landscape" (needs the Modify system settings permission; rotation is restored afterwards).
- Unit tests for the home layout rules and widget size limits.

### Fixed
- Debounced, race-safe app list reloads on install, update and removal; icons refresh after an update; home and dock entries for uninstalled apps are pruned.

### Removed
- The drawer's sort menu and Recent filter, the dock's recent-apps button, and their Settings entries. The drawer is always A to Z.

### Known limits (Android does not allow these for a launcher)
- A launcher cannot force a portrait-only app into landscape, show a taskbar of running apps, or be a full file manager. Files / Storage / Terminal shortcuts and a recent-apps panel are provided instead.
- Dragging a shortcut to a different home page is not supported yet; drops work within a page.
- This build has not yet been compiled or tested on a device; CI will run the unit tests and build the APK.

## 1.0.0-preview — 2026-10-10

### Added
- Initial landscape-first Android launcher release preview.
- macOS-inspired frosted-glass interface.
- Home screen, app drawer, dock, and launcher settings.
- Wallpaper selection, widget picker, OTG shortcut, and optional keyboard shortcuts.
- GitHub Actions builds for debug and signed release-preview APK artifacts.

### Notes
- This is a preview build; test it on your device before using it as your default launcher.
- The CI preview APK uses a temporary signing key. Future builds may require uninstalling the previous preview before installing the next one.
- This changelog describes the repository build configuration; it does not claim independent device testing of every feature.
