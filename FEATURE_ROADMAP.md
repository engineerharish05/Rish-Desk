# Rish Deskdroid — Feature Roadmap and Acceptance Checklist

This file is the source of truth for the feature-by-feature implementation loop. A feature is not complete merely because its UI exists: it needs implementation, error handling, automated checks where practical, and documented device-test requirements. Keep status accurate and update this file with every feature pull request.

## 1. Launcher foundation and app lifecycle
- [ ] Set Rish Deskdroid as the Android Home app and handle Home/Recents/lifecycle transitions safely.
- [ ] Discover launchable applications and refresh on package install, update, removal, and enable/disable events.
- [ ] Launch applications reliably; handle missing launch intents, work profiles, and uninstalled packages without crashing.
- [ ] Persist user preferences and recover from corrupted, missing, or outdated preference values.
- [ ] Avoid stale app-query results, duplicate receivers, leaked executors, and unnecessary repeated package scans.
- [ ] Respect Android package-visibility rules and request only necessary permissions.

## 2. Home screen and app organization
- [ ] Display a responsive home grid on portrait and landscape screens without clipped icons, labels, or controls.
- [ ] Add, remove, move, and reorder app shortcuts using drag-and-drop.
- [ ] Persist shortcut positions and recover gracefully if an app is removed.
- [ ] Create, rename, open, reorder, and remove folders; move apps into and out of folders.
- [ ] Configure grid columns/rows, icon size, label visibility, spacing, and page count.
- [ ] Support horizontal paging, page indicators, and sensible empty-page cleanup.
- [ ] Support a configurable dock, reorder dock items, and handle dock capacity consistently.
- [ ] Provide a stable default home layout and reset-to-default action.

## 3. App drawer
- [x] Search apps by label or package name without case sensitivity.
- [x] Sort applications A–Z, Z–A, or favorites-first.
- [x] Mark favorites and persist favorite state.
- [x] Hide apps from the normal drawer and restore them from the hidden-app view.
- [ ] Ensure hidden apps are never confused with uninstalling or disabling an app.
- [ ] Add robust empty states, clear-search action, and keyboard/IME behavior.
- [ ] Test very long app names, large app counts, package changes, and unusual characters.
- [ ] Ensure grid sizing and touch targets work in landscape and on small displays.

## 4. Gestures and navigation
- [ ] Define configurable home gestures (e.g. swipe up/down and double tap) with conflict-free defaults.
- [ ] Add reliable back, Home, drawer open/close, paging, and dismissal behavior.
- [ ] Prevent accidental gestures from blocking normal taps, text entry, scrolling, or accessibility services.
- [ ] Provide optional haptic feedback with a user setting.

## 5. Search and quick actions
- [ ] Add a launcher-level search surface with app results and keyboard focus.
- [ ] Make search results open the correct app or supported Android system destination.
- [ ] Add quick actions only where Android APIs and permissions support them; show clear explanations otherwise.
- [ ] Handle no results, rapid typing, keyboard dismissal, and configuration changes.

## 6. Widgets and desktop panels
- [ ] Add, resize where supported, move, and remove Android App Widgets using the supported AppWidget APIs.
- [ ] Persist widget IDs and positions; clean up deleted or invalid widget IDs.
- [ ] Handle widget-provider configuration and permission flows safely.
- [ ] Respect the widgets enabled setting and provide an understandable empty state.
- [ ] Test widget host lifecycle, rotation, activity recreation, and provider removal.

## 7. Wallpaper, theme, and appearance
- [ ] Select and apply a wallpaper through supported Android APIs.
- [ ] Keep wallpaper changes and wallpaper-loading failures from crashing the launcher.
- [ ] Provide a consistent light/dark/system theme strategy where supported by the design.
- [ ] Apply glass/transparency opacity settings consistently and clamp invalid values.
- [ ] Add configurable icon/label appearance, accent colors, and spacing.
- [ ] Check readability, contrast, motion sensitivity, and accessibility on different backgrounds.

## 8. Settings and configuration
- [ ] Make every visible setting change real application behavior, or clearly label unsupported options.
- [ ] Persist settings and validate values before saving.
- [ ] Add confirmation for destructive/reset actions.
- [ ] Explain why special permissions are requested and provide a route to the relevant Android settings page.
- [ ] Verify all settings survive process death and device restart where applicable.

## 9. Keyboard, accessibility, and large-screen use
- [ ] Support D-pad/keyboard focus and activation for core launcher actions.
- [ ] Add meaningful content descriptions and predictable focus order.
- [ ] Support font scaling and Android accessibility services.
- [ ] Verify layout on short landscape screens, tablets, split-screen, and different densities.
- [ ] Ensure touch targets and labels remain usable when icon size is reduced.

## 10. Files, USB/OTG, and system integrations
- [ ] Treat OTG/USB status as informational unless the required Android API and permission are available.
- [ ] Add safe shortcuts to Android file-management destinations only where the platform supports them.
- [ ] Handle USB attach/detach broadcasts without duplicate receivers or lifecycle leaks.
- [ ] Never claim the launcher can bypass Android sandboxing, storage restrictions, or other apps' security boundaries.

## 11. Performance, resilience, and privacy
- [ ] Keep package queries, image decoding, and expensive work off the main thread.
- [ ] Cache icons safely and invalidate caches when packages change.
- [ ] Avoid unbounded background work, duplicate listeners, and unnecessary recomposition.
- [ ] Handle exceptions at system boundaries with useful recovery states and diagnostic logs.
- [ ] Do not collect or transmit personal usage data unless explicitly designed, disclosed, and consented to.
- [ ] Verify startup time, memory use, battery impact, and behavior with many installed apps.

## 12. Automated quality gates
- [ ] Run unit tests for pure logic on every push and pull request.
- [ ] Build debug and release variants on every pull request to main.
- [ ] Verify the generated APK exists and its signature is valid for preview builds.
- [ ] Inspect actual build/test logs and fix root causes rather than retrying unchanged failures.
- [ ] Add regression tests for every bug that can be represented in an automated test.
- [ ] Keep changes isolated in feature branches and use pull requests for review.
- [ ] Never merge a change with failing required checks or claim physical-device testing that did not occur.
- [ ] Before publication, test install, update, default-home selection, app launch, settings persistence, widgets, permissions, rotation, and recovery on a physical Android device.

## Implementation-loop rules
1. Read this checklist and inspect the current code before selecting work.
2. Choose one coherent unfinished feature or a small tightly related group.
3. Implement actual behavior, not a placeholder-only UI.
4. Add unit tests or other automated regression checks where practical.
5. Run CI. If it fails, read the logs, fix the root cause, and rerun; cap each failure at three targeted attempts before documenting a blocker.
6. Open/update a pull request only when the scope is clear. Do not merge unless checks pass.
7. Update checkboxes only when the stated acceptance criteria are met. If device verification is still needed, leave the feature unchecked and record that limitation.
8. Repeat with the next highest-priority item. The project is not publication-ready until the applicable checklist is satisfied and device testing is complete.

## Platform boundary
A launcher can only perform actions permitted by Android APIs, declared permissions, and user-granted special access. Floating windows, system controls, file access, widget behavior, and interactions with other apps must not be represented as fully supported unless verified against the target Android versions.


## Latest engineering pass — 2026-10-12

- **App launch recovery (in review):** launcher actions now check whether the selected activity still exists and whether both the activity and its application are enabled before attempting to start it. Expected missing-target and permission failures produce a short user-facing message instead of escaping the launch handler.
- **Automated coverage added:** pure launch-readiness decision tests cover missing, disabled, and available targets.
- **Verification limitation:** keep the app-launch checklist item above unchecked until CI passes and package removal/disable/re-enable behavior is exercised on a physical Android device.
