# FocusFlow

**Focus better. Work smarter.**

FocusFlow is a minimalist, offline-first Pomodoro timer for Android, built with Kotlin and
Jetpack Compose by [IgnitronDev](https://ignitrondev.space). No account, no tracking, no
network permission: all data stays on the device.

> Status: early development (Phase 1, app foundation). See [docs/ROADMAP](docs/REPOSITORY_AUDIT.md#8-phase-plan).

## Build

```bash
cd FocusFlow
./gradlew assembleDebug
```

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for requirements, tests and conventions.

## Tech stack

Kotlin · Jetpack Compose · Material 3 (adaptive navigation) · Navigation Compose · Hilt ·
Coroutines/Flow · DataStore · Room (from Phase 2) · JUnit · Robolectric.

## License and attribution

FocusFlow is licensed under the [Apache License 2.0](LICENSE); see [NOTICE](NOTICE) for attribution.

Contributions are welcome: read [CONTRIBUTING.md](CONTRIBUTING.md), the [Code of Conduct](CODE_OF_CONDUCT.md) and the [security policy](SECURITY.md).

It lives in IgnitronDev's fork of [android/compose-samples](https://github.com/android/compose-samples)
(Copyright The Android Open Source Project, Apache-2.0). FocusFlow reuses that repository's build
setup and architectural patterns; the upstream samples and their notices are unchanged.
