# Contributing to RishDesk

Thank you for taking the time to improve RishDesk. Keep changes focused, explain the reason for each change, and avoid combining unrelated fixes in one pull request.

## Before making changes

1. Read the [project README](README.md) and [repository structure guide](docs/REPOSITORY_STRUCTURE.md).
2. Check open pull requests and existing issues to avoid duplicating work.
3. Create a focused branch from `main`, for example `fix/drawer-search` or `docs/readme-update`.
4. Do not commit generated build output, local secrets, signing keys, passwords, or personal device data.

## Code changes

- Preserve the existing Kotlin, Jetpack Compose, and Gradle conventions.
- Keep UI, app data, and platform integrations in their existing responsibility areas unless a refactor is necessary.
- Avoid changing the Android application ID/package name unless the change explicitly requires it and its migration impact is documented.
- Keep accessibility in mind: provide useful labels and adequate touch targets.
- Keep changes compatible with the project's declared Android minimum SDK.
- Add or update unit tests for behavior changes where practical.
- Do not claim a device-specific behavior has been verified unless it was tested on a real device.

## Verify before opening a pull request

Run the checks available in your environment:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

If you cannot run a check locally, say so in the pull request. GitHub Actions results should be checked before merging.

## Pull request checklist

- [ ] The change has one clear purpose.
- [ ] Related tests and documentation are updated.
- [ ] No secrets, signing files, generated APKs, or unrelated files are included.
- [ ] The app name, application ID, permissions, and launcher behavior are not changed accidentally.
- [ ] Build/test results and any unverified device-specific behavior are stated honestly.
- [ ] Screenshots or a short reproduction guide are included for visible UI changes.

Use the pull request template and link related issues when applicable.
