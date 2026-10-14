# Repository Structure

This guide explains where project files belong. It is documentation only: existing source files and branches are preserved.

## Top level

```text
Rish-Desk/
├── .github/
│   ├── workflows/             # CI/build automation
│   └── PULL_REQUEST_TEMPLATE.md
├── app/                       # Android application module
│   ├── build.gradle.kts       # App build configuration
│   ├── proguard-rules.pro     # R8/ProGuard rules
│   └── src/
│       ├── main/              # Production app source and resources
│       └── test/              # Local unit tests
├── docs/                      # Maintainer and project documentation
├── gradle/                    # Gradle wrapper configuration
├── .gitignore
├── build.gradle.kts           # Root build configuration
├── gradle.properties
├── gradlew / gradlew.bat      # Gradle wrapper scripts
├── settings.gradle.kts
├── README.md                  # Project overview and getting started
├── CONTRIBUTING.md            # Contribution workflow
├── SECURITY.md                # Private vulnerability reporting guidance
├── CHANGELOG.md               # User-facing change history
├── FEATURE_ROADMAP.md         # Feature planning and progress
└── LICENSE                    # Project license
```

The tree above describes intended roles. Consult the live repository for exact files and paths; this guide does not imply that every listed directory must contain files in every branch.

## Android source

The main Kotlin package currently lives under:

`app/src/main/java/com/rishdeskdroid/app/`

Keep responsibilities grouped by purpose:

- **Activity and app entry:** activity lifecycle and launcher entry points.
- **data/**: app discovery, persisted launcher preferences, wallpaper/widget state, and platform controllers.
- **ui/**: Compose screens, reusable visual components, dialogs, theme, and launcher surfaces.
- **res/**: Android manifest, strings, icons, themes, and other platform resources.
- **test/**: unit tests for pure logic and behavior that can be tested without a device.

Prefer adding a focused file in the appropriate existing area rather than creating duplicate implementations.

## Where new files belong

| File type | Preferred location |
| --- | --- |
| User-facing setup and usage | `README.md` |
| Contribution and review process | `CONTRIBUTING.md` |
| Security reporting | `SECURITY.md` |
| Architecture and maintainer notes | `docs/` |
| Build/test automation | `.github/workflows/` |
| Pull request checklist | `.github/PULL_REQUEST_TEMPLATE.md` |
| Android application code | `app/src/main/` |
| Unit tests | `app/src/test/` |
| Release history | `CHANGELOG.md` |
| Planned features | `FEATURE_ROADMAP.md` |

## Repository hygiene

- Keep the root directory reserved for essential project entry points and Gradle files.
- Do not commit `build/`, `.gradle/`, generated APKs, IDE-local state, secrets, or signing keystores.
- Keep CI definitions in `.github/workflows/`.
- Avoid moving Gradle files, Android resources, manifests, or Kotlin package paths merely for visual neatness; those paths may be required by the build.
- Keep experimental work on clearly named branches and use pull requests to review changes before merging.
- Do not delete or rewrite branches as part of routine organization without a separate review.
