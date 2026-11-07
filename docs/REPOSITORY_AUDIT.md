# Repository Audit — FocusFlow

Audit date: 2026-11-07 · Branch audited: `main` @ `25631e8f` (in sync with `android/compose-samples` `main`).

## 1. Repository structure

`ignitrondev/FocusFlow` (renamed from `ignitrondev/compose-samples` on 2026-11-07) is a GitHub
**fork** of `android/compose-samples`. It is not a
single Gradle build: every sample is an **independent Gradle project** with its own wrapper,
`settings.gradle.kts`, version catalog, Spotless config and debug keystore.

```
compose-samples/
├── Jetcaster/  Jetchat/  JetLagged/  JetNews/  Jetsnack/  Reply/   ← standalone Gradle builds
├── scripts/            gradlew_recursive.sh, verify_samples.sh, updateDeps.sh, format.sh, checksum.sh
├── .github/workflows/  one workflow per sample + build-sample.yml (reusable) + Release.yml + others
├── LICENSE             Apache-2.0
├── CODE_OF_CONDUCT.md, CONTRIBUTING.md (Google CLA), CODEOWNERS (@android/devrel-compose)
└── renovate.json, .github/dependabot.yml
```

`scripts/gradlew_recursive.sh` runs every `gradlew` it finds, so a new top-level project is
picked up automatically by `verify_samples.sh`.

## 2. Available samples

| Sample | Modules | DI | Persistence | Navigation | Relevant to FocusFlow |
|---|---|---|---|---|---|
| **Jetcaster** | mobile, tv, wear, glancewidget, core:{data, domain, designsystem, *-testing} | **Hilt** | **Room** | navigation-compose | Layered data/domain architecture, Hilt modules, dispatcher qualifiers, Room DAO + transaction runner, use-case unit tests |
| **JetLagged** | app | none | none (fake data) | navigation-compose | Custom Canvas graphs (sleep/heart-rate) — reference for the statistics charts |
| Jetchat | app | none | none | Navigation 3 | Chat UI; not relevant |
| JetNews | app | none | in-memory fake | Navigation 3 + nav-compose | Adaptive list/detail; partially relevant (adaptive layout) |
| Jetsnack | app | none | none | navigation-compose | Custom design system; animation reference |
| Reply | app | none | none | navigation-compose | Adaptive navigation (rail / bottom bar) reference |

No sample contains timer, alarm, notification or DataStore code.

## 3. Recommended foundation

**Create a new standalone project `FocusFlow/` next to the existing samples**, instead of
renaming or rewriting one of them.

* None of the samples is a functional match (no timer/notification/settings code), so turning
  one into FocusFlow would mean deleting most of its code — the opposite of "reuse working code".
* Keeping the samples untouched keeps the fork rebaseable on upstream.
* FocusFlow follows the repository's own conventions, so it fits in like the other projects:
  standalone Gradle build, same wrapper (Gradle 9.5.0), same plugin set, Spotless + ktlint, the
  reusable CI workflow pattern.

What is reused, and from where:

| From | Reused |
|---|---|
| JetLagged / Jetchat | Gradle wrapper, root build + Spotless setup, `app` build-file structure, catalog entries and versions |
| Jetcaster | Hilt + KSP setup, `Dispatcher` qualifier pattern, Room/DAO + repository layering, use-case + fake-repository unit-test style |
| JetLagged | Canvas chart approach for statistics (Phase 5) |
| Reply | Adaptive navigation idea (implemented with `NavigationSuiteScaffold`) |

**Module strategy:** a single `:app` module with the proposed package structure
(`core/ data/ domain/ feature/ navigation/`). The app is small and this keeps Gradle
configuration cheap on low-memory dev machines; the `domain` package stays free of Android
types so it can be split into a pure-Kotlin module later without changes.

## 4. Existing toolchain and dependency versions

From the samples' `gradle/libs.versions.toml` (shared across samples):

| Item | Version |
|---|---|
| Gradle wrapper | 9.5.0 |
| AGP | 9.3.1 (Jetcaster 9.2.1) |
| Kotlin | 2.4.20 (Jetcaster 2.3.21), KSP 2.3.12 |
| Compose BOM | 2026.09.00 — **`compose-bom-alpha`** artifact |
| compileSdk / minSdk / targetSdk | 37 / 24 / 33 (Jetchat 37, Jetsnack 36) |
| Hilt | 2.60.1 · androidx.hilt 1.4.0 |
| Room | 2.8.5 |
| navigation-compose | 2.10.1 · Navigation 3 1.2.0 |
| lifecycle | 2.11.0 (plus a stale `androidx-lifecycle = 2.8.2` key) |
| Coroutines | 1.11.0 |
| JUnit 4.13.2 · Robolectric 4.17 · androidx.test 1.7.0 | |
| JDK target | 17 |

Decisions for FocusFlow:

* **Stable Compose BOM**: use `androidx.compose:compose-bom:2026.09.00` (stable, same version
  number) instead of the alpha BOM used by the samples.
* **targetSdk 37** (current; Play requires recent target levels), compileSdk 37, minSdk 26.
  minSdk 26 removes the pre-Oreo notification-channel branches and covers practically every
  device in use; it is easy to lower to 24 if needed.
* **New dependency:** `androidx.datastore:datastore-preferences:1.2.1` (not in any sample
  catalog; latest stable on Google Maven at audit time; 1.3.0 is newer and can be adopted
  after a check).
* FocusFlow keeps **its own catalog**. The sample catalogs are kept in sync by
  `scripts/updateDeps.sh`, which lists the samples explicitly, so FocusFlow is not touched by it.
* **No Google Play Services / Firebase / Google Fonts downloadable fonts**, to keep the app
  F-Droid-compatible and fully offline.

## 5. License and attribution

| Item | License | Obligation for FocusFlow |
|---|---|---|
| Repository code (`LICENSE`) | Apache-2.0, © The Android Open Source Project | Keep `LICENSE` and the existing headers on every upstream file |
| Sample assets (`*/ASSETS_LICENSE`) | Fonts: SIL OFL 1.1; other assets: Apache-2.0 | If a sample font is ever reused, ship the OFL text with it |
| `CONTRIBUTING.md` | Refers to the Google CLA | Applies to upstream; FocusFlow gets its own `CONTRIBUTING.md` (no CLA) |
| `CODEOWNERS` | `@android/devrel-compose` | Upstream team; irrelevant in the fork, left untouched |

**Decision (approved 2026-11-07):** FocusFlow code is **Apache-2.0**, new files carry
`Copyright <year> IgnitronDev`. Files adapted from a sample keep their original Google header,
plus a note that they were modified (Apache-2.0 §4(b)). The repository as a whole is **not**
relicensed. `FocusFlow/LICENSE` (unmodified Apache-2.0 text) and `FocusFlow/NOTICE` (attribution) were added on 2026-11-07.

## 6. Proposed modifications

Added (no upstream files deleted or rewritten):

```
FocusFlow/                       standalone Gradle project, applicationId com.ignitrondev.focusflow
.github/workflows/FocusFlow.yaml CI: spotless, lint, assembleDebug, unit tests (Phase 1)
```

Minimal edits to upstream infrastructure, needed so the fork's CI behaves:

* `default-check.yml`: add `FocusFlow/**` to `paths-ignore`, like the other projects.
* **Not changed (yet):** `Release.yml` triggers on any `v*` tag and builds the upstream samples
  with keystore secrets that the fork does not have. FocusFlow releases will use
  `focusflow-v*` tags and their own workflow (Phase 7).

## 7. Technical risks

| Risk | Impact | Mitigation |
|---|---|---|
| Process death kills ViewModel coroutines | Timer stops / completion lost | Timer state persisted in Room as a deadline; UI recomputes from the clock; completion is driven by a system alarm, not a coroutine |
| Wall-clock changes (manual time change, NTP, timezone) | Wrong remaining time | Active countdown uses `SystemClock.elapsedRealtime()` (monotonic, counts deep sleep); wall clock is only used for history timestamps and post-reboot recovery |
| Reboot resets `elapsedRealtime` and clears alarms | Lost timer | Persist boot count (`Settings.Global.BOOT_COUNT`) with the timer; on a different boot, recover from the wall-clock deadline, clamped to the planned duration; reschedule on `BOOT_COMPLETED` |
| Exact alarms restricted: `SCHEDULE_EXACT_ALARM` not granted by default on Android 14+; `USE_EXACT_ALARM` limited by Play policy | Late completion notification | Use exact alarm only when `canScheduleExactAlarms()`; otherwise `setAndAllowWhileIdle` and say so in the UI and docs. Never promise exact timing without the grant |
| Foreground services need a justified type on Android 14+; no type fits a countdown well | Policy rejection / crashes | No FGS for the MVP. Ongoing notification uses the system countdown chronometer (`setUsesChronometer` + `setChronometerCountDown`), which ticks without our process running |
| `POST_NOTIFICATIONS` runtime permission (Android 13+) | Silent completion | Ask in context (first timer start), degrade gracefully, show a status in Settings |
| Duplicate completion (alarm + open UI both finishing a session) | Double history rows | Completion is one Room transaction guarded by the timer's run ID; second attempt is a no-op |
| Laptop has 7.6 GB RAM | Slow/killed Gradle builds | Single module, `-Xmx2g`, in-process Kotlin compiler for local builds |
| No emulator system image installed locally | Instrumented tests cannot run locally | Compose UI tests run on Robolectric in `test/`; instrumented tests go to CI with an emulator |
| Upstream `Release.yml` / Gemini review workflow run in the fork | Failing runs on tags / PRs | Separate tag prefix; document; disable in the fork only with approval |

## 8. Phase plan

| Phase | Scope | Exit criteria (verified by commands) |
|---|---|---|
| 0 Audit | This document | — |
| 1 Foundation | Gradle project, app identity, Hilt, design system (light/dark/system), adaptive navigation with 4 destinations, theme preference in DataStore, CI workflow | `spotlessCheck lintDebug assembleDebug testDebugUnitTest` green |
| 2 Timer engine | Pure-Kotlin state machine, `TimeSource` abstraction, deadline math, Room `timer_state`, start/pause/resume/reset/skip, recovery rules | Unit tests for every transition, recovery and duplicate prevention |
| 3 Android integration | AlarmManager completion receiver, notification channels + permission flow, chronometer notification, boot/time-change receivers | Robolectric tests for scheduling decisions; manual run on device |
| 4 Persistence | `focus_sessions` history, settings in DataStore, exported Room schemas + migration tests | DAO tests, migration test |
| 5 Features | History (grouped by date), statistics (today/week, average), configurable durations, sound/vibration | ViewModel + statistics unit tests, UI tests |
| 6 QA | Accessibility (TalkBack labels, touch targets, font scale), performance (baseline profile), coverage gaps | Lint clean, tests green, manual checklist |
| 7 Release | README, screenshots, CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, LICENSE/NOTICE, issue templates, release workflow with signing secrets | Signed AAB/APK from CI on a `focusflow-v*` tag |
