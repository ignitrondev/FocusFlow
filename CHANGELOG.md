# Changelog

All notable changes to FocusFlow. Versions follow [Semantic Versioning](https://semver.org).

## Unreleased

### Fixed
- Settings → Notifications → Alert sound no longer closes on Android 17 after an alarm tone was
  picked: Google's sound picker crashed on the combined "notification or alarm" request. The picker
  now lists every system sound and your own.

## 1.0.0 — 2026-11-08

First stable release.

### Added
- Achievements: 13 badges for completed sessions, streaks and hours of focus, in bronze, silver,
  gold and legendary. An unlocked badge shows its date; a locked one shows how far along you are.
  They stay on the device, and a notification announces each new one when notifications are
  allowed (Android and iOS). Achievements stay in English in every language.
- Bahasa Indonesia. FocusFlow follows the device language, or the app language chosen in
  Android 13+ settings.
- Settings → About → Check for updates opens the latest release on GitHub. FocusFlow itself still
  has no internet permission.

### Changed
- The phone navigation bar floats over the screen as a Liquid Glass pill, with each icon above its
  label, and screens scroll underneath it. The glass refracts the screen on Android 13+ and iOS,
  blurs it on Android 12, and is a translucent surface on older Android. The selected tab fades
  in, or switches at once when animations are turned off.
- Wallpaper colors (Material You) are on by default on Android 12+. The selected tab, buttons and
  statistics follow them; Settings → Appearance turns them off.
- Statistics are redesigned: summary cards, a chart of the week with an hour axis, and cards for
  the daily average, average session and streak. They follow the app theme (light or dark) and its
  colors, and Share is a round tonal button.
- Achievements open from the end of Statistics, with a round Back button.

## Earlier changes (shipped in the 1.0.0 betas)

### Fixed
- The end of a session, and an automatic break, could be up to ~18 minutes late on Android 14+
  while the app was not open: the alarm was inexact unless the user allowed "Alarms & reminders".
  FocusFlow now uses exact alarms (`USE_EXACT_ALARM`, granted at install on Android 13+).

### Added
- Share a Strava-style statistics card (today or this week) through Android's share sheet:
  a 1080×1350 image with focus time, sessions, daily average, streak and the last 7 days.
- The card shows your role (e.g. "Software Engineer", asked once and remembered) and can use a
  photo of where you are working as its background, taken with the camera app or picked with
  Android's photo picker. No camera or storage permission is needed.
- Two card formats: feed post (4:5, 1080×1350) and story (9:16, 1080×1920), whose content stays
  clear of the areas Instagram covers with the account name, music and reply bar.
- Current streak: consecutive days with a completed focus session.
- Settings → About credits the author.

- Pick the alert sound with Android's sound picker (Settings → Notifications → Alert sound),
  including silent and any alarm or notification sound.
- The timer notification shows the time left in large digits, in both the collapsed and the
  expanded notification.

## 1.0.0-beta.4 — 2026-11-08

### Fixed
- Statistics use one locale week for the label, the chart and the weekly totals. On Sunday 8 Nov 2026
  with a Monday week start the range is 2–8 Nov, and the first weekday comes from the device calendar
  on Android and iOS.
- A streak counts consecutive days with a completed focus session, ending today or yesterday.
  No completed session means a streak of 0.
- Sharing with no focus time keeps the Share button. The card shows "Start your first session"
  instead of zeros.

### Changed
- On phones the bottom navigation is a segmented bar: a rounded container, with the active tab in an
  orange capsule around its icon and label. Tablets and phones in landscape keep the side rail.

## 1.0.0-beta.2 — 2026-11-07

### Changed
- On phones the navigation bar is a floating glass pill with rounded corners. Tablets and phones
  in landscape keep the side rail.

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
