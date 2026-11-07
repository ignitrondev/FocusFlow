# Changelog

All notable changes to FocusFlow. Versions follow [Semantic Versioning](https://semver.org).

## Unreleased

### Fixed
- The end of a session, and an automatic break, could be up to ~18 minutes late on Android 14+
  while the app was not open: the alarm was inexact unless the user allowed "Alarms & reminders".
  FocusFlow now uses exact alarms (`USE_EXACT_ALARM`, granted at install on Android 13+).

### Added

- Pick the alert sound with Android's sound picker (Settings → Notifications → Alert sound),
  including silent and any alarm or notification sound.
- The timer notification shows the time left in large digits, in both the collapsed and the
  expanded notification.

## 1.0.0-beta.1 — 2026-11-07

First public beta.

### Timer
- Pomodoro cycle: focus, short break, and a long break after every 4 focus sessions (adjustable).
- Start, pause, resume, reset and skip. The session type is locked while a session is in progress.
- Deadline-based: correct across app restarts, screen off, wall-clock changes and reboots.
- Sessions that end while the app is closed are recorded with their real end time.

### Alerts
- Alert with sound and vibration when a session ends, also with the app closed (no foreground
  service). Exact timing when "Alarms & reminders" is allowed; otherwise it may be a few minutes
  late in deep idle.
- Ongoing countdown notification with Pause, Resume and Reset.
- Restored after a reboot or an app update.

### History and statistics
- History grouped by day, with interrupted sessions marked.
- Statistics: today, this week, daily average, average session, all time, last 7 days chart.

### Settings
- Focus, short and long break durations; sessions before a long break; automatic start.
- Theme (system, light, dark) and wallpaper colors on Android 12+.
- Notification and precise-alert status with links to Android settings.

### Quality
- 87 JVM tests (engine, persistence, alarms, notifications, UI, accessibility, contrast).
- Large font sizes, landscape phones, foldables and tablets.

## 0.2.0 — 2026-11-07 (preview, not published)
- Working timer engine with Room persistence.

## 0.1.0 — 2026-11-07 (preview, not published)
- App foundation: theme, navigation, settings.
