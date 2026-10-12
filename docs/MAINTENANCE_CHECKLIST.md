# Maintenance Checklist

Use this checklist to keep the repository reviewable without deleting historical work.

## Before merging a pull request

- Confirm the pull request targets `main` and has a single understandable purpose.
- Review the full diff, including generated files and manifest/resource changes.
- Check unresolved review comments and resolve each finding with a fix or a documented reason.
- Confirm CI ran for the latest head commit; a successful run on an older commit is not sufficient.
- Run unit tests and assemble a debug APK when possible.
- Check for duplicate or conflicting implementations in other open pull requests.
- Verify app label, launcher icon, package/application ID, minimum SDK, and permissions.
- State whether physical-device behavior was tested or remains unverified.

## Branch organization

Use descriptive prefixes for new branches:

- `feature/` — user-facing capability
- `fix/` — bug fix
- `chore/` — repository maintenance or tooling
- `docs/` — documentation-only work
- `test/` — test-only work
- `integration/` — coordinated integration work

Keep existing branches until their changes have been compared with `main` and any open pull request. Do not delete branches as part of this cleanup.

## Release preparation

- Confirm the intended version and update `CHANGELOG.md`.
- Verify the exact commit being released.
- Check the release workflow and inspect the generated artifact.
- Confirm signing is configured appropriately for the intended release.
- Publish installation instructions and known limitations.
- Do not commit release keystores or signing credentials.
