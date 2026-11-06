# Contributing to FocusFlow

Thanks for helping make FocusFlow better! Bug reports, ideas, translations and code are all
welcome.

> FocusFlow lives in the `FocusFlow/` folder of IgnitronDev's fork of `android/compose-samples`.
> This guide covers FocusFlow only. The repository-root `CONTRIBUTING.md` belongs to the upstream
> samples (and their Google CLA) and does **not** apply to FocusFlow.

## Ground rules

* Be kind. Everyone taking part must follow the [Code of Conduct](CODE_OF_CONDUCT.md).
* FocusFlow is **offline-first and privacy-first**: no analytics, no trackers, no mandatory
  account, no network access. Changes that add those will not be accepted.
* Security problems: please do **not** open a public issue, see [SECURITY.md](SECURITY.md).

## License of contributions

FocusFlow is licensed under the [Apache License 2.0](LICENSE). By submitting a pull request you
agree that your contribution is licensed under the same license (Apache-2.0 section 5). No CLA
is required.

## Making a change

1. Fork the repository and create a branch from `main`, e.g. `focusflow/fix-timer-reset`.
2. Keep the change focused. For larger features, open a discussion or issue first.
3. Follow the conventions in [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md):
   * Kotlin and Jetpack Compose only for new code.
   * Business logic stays outside composables; `domain/` has no Android imports.
   * All user-facing text goes in `strings.xml`.
   * Add or update tests for behavior you change.
4. Run the same checks as CI from the `FocusFlow/` folder:

   ```bash
   ./gradlew spotlessApply
   ./gradlew spotlessCheck lintDebug assembleDebug testDebugUnitTest -PwarningsAsErrors=true
   ```

5. Open a pull request that explains **what** changed and **why**, with screenshots for UI
   changes (light and dark theme).

## Translations

Add a `values-<language>/strings.xml` under `app/src/main/res/` and the locale to
`res/xml/locales_config.xml`. Strings marked `translatable="false"` stay as they are.
