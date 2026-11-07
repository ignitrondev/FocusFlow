# Roadmap

## Done (1.0.0-beta.1)

| Phase | Scope |
|---|---|
| 0 | Repository audit ([REPOSITORY_AUDIT.md](REPOSITORY_AUDIT.md), historical) |
| 1 | Foundation: design system, adaptive navigation, settings, CI |
| 2 | Deadline-based timer engine, Room persistence, recovery |
| 3 | Alarms and notifications for a closed app, permissions, reboot |
| 4 | Settings in DataStore, schema guard for Room |
| 5 | History, statistics, timer settings |
| 6 | Accessibility and layout QA ([QA.md](QA.md)) |
| 7 | Documentation, templates, release workflow, beta |

## Before 1.0

- Beta feedback; the manual device checklist in [QA.md](QA.md) on several Android versions and
  manufacturers.
- Release signing in CI (repository secrets) so releases are built reproducibly from tags.
- F-Droid submission (metadata is in `fastlane/`).
- Baseline profile for faster startup (needs a device or emulator in CI).

## Ideas after 1.0

- Translations (Indonesian first).
- Export and import of history (CSV/JSON), included in Android backup.
- Home screen widget and quick settings tile.
- Wear OS companion.
- Optional focus sound (white noise), still fully offline.
