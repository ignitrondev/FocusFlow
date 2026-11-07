# Quality checks

What is verified automatically on every CI run, and what still needs a real device before a
release.

## Automated (CI: `./gradlew spotlessCheck lintDebug assembleDebug testDebugUnitTest`)

| Area | How | Where |
|---|---|---|
| Timer engine | Every transition, the long-break cycle, pause accounting, wall-clock changes, reboot recovery (with and without boot count), completion at the real deadline, auto-start chains, settings changes | `TimerEngineTest` |
| Persistence | Process recreation, sessions completed while closed, concurrent completion recorded once, interrupted sessions, corrupt rows, settings clamping | `RoomTimerRepositoryTest`, `DataStore*Test` |
| Database schema | Database built from the committed schema JSON must open with the current entities; negative-tested with a tampered hash | `DatabaseSchemaTest` |
| Alarms and notifications | Monotonic-clock alarm, exact/inexact fallback, notification content and channels, no posting without permission, deadline alarm end to end (one record, one alert), notification buttons, boot recovery | `platform/*Test` |
| Statistics and history | Completed focus only, locale week start, last 7 days, local-day grouping | `FocusStatisticsCalculatorTest`, `GroupByDayTest` |
| UI flows | Start/pause/resume/reset, countdown, locked session type, settings → timer, history and statistics populated, theme persistence, navigation | `FocusFlowAppTest` (Robolectric) |
| Accessibility: labels and touch targets | Every clickable element on every screen (with data, scrolled) has a label and a 48×48 dp target; negative-tested | `AccessibilityTest` |
| Accessibility: contrast | WCAG ratios for the text/background pairs used, light and dark | `ColorContrastTest` |
| Static analysis | Android lint with warnings as errors (only "newer version available" notices remain, kept aligned with upstream samples), ktlint | CI |

Compose's built-in ATF checks (`enableAccessibilityChecks()`) are skipped on Robolectric, which is
why the label/touch-target checks are implemented directly on the semantics tree.

## Visually reviewed (Robolectric screenshots, 2026-11-07)

Phone portrait, light and dark; phone landscape (navigation rail); tablet 1280×800 (rail, wide
layout); 200 % font size on every screen. Fixes that came out of it: segmented and navigation
labels shrink to fit instead of being cut off, timer digits scale with the dial, landscape phones
use the rail instead of a bottom bar.

## Manual checklist before a release (real device)

- [ ] Fresh install: notification permission is asked on the first Start, not at launch.
- [ ] Start a 1-minute short break, lock the phone: the alert sounds on time.
- [ ] Same with the app swiped away from Recents.
- [ ] "Precise alerts" off (Android 14+ default): note how late the alert is when idle.
- [ ] Start a session, reboot the phone: the countdown notification and alarm come back.
- [ ] Change the system time while running: the countdown does not jump.
- [ ] Notification buttons: Pause, Resume, Reset.
- [ ] TalkBack: navigate the timer, history and statistics screens.
- [ ] Dark theme and wallpaper colors (Android 12+).

## Known gaps

* No baseline profile yet: generating one needs a device or emulator (Macrobenchmark). Startup
  relies on R8 and Compose's bundled profiles.
* No instrumented (on-device) tests in CI; everything runs on the JVM with Robolectric.
* Room data is not included in Android backup (only settings are).
