# Timer engine

FocusFlow's timer is **deadline-based**. Nothing counts down in memory: the database stores how
much time was left at a known moment, and every screen derives the remaining time from the clock.
Leaving the screen, rotating the device or the process being killed cannot lose or skew a session.

Code: `domain/timer/` (pure Kotlin, no Android imports) and `data/timer/` (Room persistence).

## Clocks

| Clock | Used for | Why |
|---|---|---|
| `SystemClock.elapsedRealtime()` | The running countdown | Monotonic and counts deep sleep. Unaffected by manual time changes, NTP or time zones. |
| `System.currentTimeMillis()` | History timestamps; recovery after a reboot | Only clock that survives a reboot. |
| `Settings.Global.BOOT_COUNT` | Reboot detection | Tells whether two `elapsedRealtime` readings are comparable. If a device does not expose it, a monotonic clock that went backwards still proves a reboot. |

All three are read together as a `TimerInstant` from the injectable `TimeSource`, so tests use a
fake clock instead of waiting.

## State

`TimerState` (one row in `timer_state`, see [DATABASE.md](DATABASE.md)):

* `status`: `Idle`, `Running` or `Paused`.
* `remainingMillis`: time left. While running, it is the time left **at `anchor`**.
* `anchor`: the clock reading taken when the timer was started or resumed.
* `runId`: unique per session run; the key that makes completion idempotent.
* `completedFocusInCycle`: focus sessions completed since the last long break.

While running: `remaining(now) = remainingMillis - (now.elapsed - anchor.elapsed)`.

## Transitions

| Action | From | Result |
|---|---|---|
| Start | Idle | Running, anchored now; start time recorded |
| Resume | Paused | Running, anchored now |
| Pause | Running | Paused with the time left; paused time never counts |
| Reset | any | Same session type, idle, full duration |
| Skip | any | Next session type, idle |
| Select type | Idle only | Chosen type, full duration. Locked once a session has started. |
| Time runs out | Running | Session recorded as completed; next session type, idle (or running with auto-start) |

Session order: focus → short break → focus → … and a **long break after every 4th completed focus
session** (configurable). Completing or skipping a long break starts a new cycle. A *skipped* focus
session does not count towards the long break.

Reset or skip after at least one minute of running time keeps the session in the history as
*interrupted* (`completed = false`). Shorter attempts are not recorded.

## Settling and duplicate prevention

Every repository operation runs one Room transaction:

1. read the stored state,
2. **settle** it: re-anchor after a reboot, then complete every session whose deadline has passed,
3. apply the user's action (if any),
4. insert the finished sessions and write the new state.

Room runs write transactions one at a time, so a second caller (another screen, and from Phase 3
the completion alarm) always sees the state the first caller left behind, which is no longer due.
The session row's primary key is the run id and inserts ignore conflicts, a second line of
defence. `RoomTimerRepositoryTest.concurrentCompletionIsRecordedExactlyOnce` covers this.

A completion is recorded at its **real deadline**, measured back from "now" with the monotonic
overshoot, even when it is noticed hours later.

## Recovery rules

| Situation | Behaviour |
|---|---|
| App backgrounded, screen off | Nothing runs; the deadline is unchanged. The screen recomputes on return. |
| Process killed, app reopened | `TimerViewModel` calls `refresh()`; sessions that ended meanwhile are recorded at their deadline. |
| Wall clock changed while running | No effect: the countdown uses `elapsedRealtime`. |
| Device rebooted while running | Remaining time is computed from the wall-clock deadline, never more than was left before the reboot, then re-anchored to the new boot's monotonic clock. |
| Session ended while the device was off | Recorded as completed at its deadline. |
| Auto-start enabled and the app was closed for a long time | Following sessions are chained from each previous deadline, for at most one full cycle (8 sessions by default); then the timer stops. |
| Unreadable stored state | Ignored; the timer starts fresh rather than crashing. |

## Alerts while the app is closed (Phase 3)

Code: `platform/`.

FocusFlow does **not** use a foreground service. Nothing needs to run during a session: the
deadline is stored, and Android wakes the app when it is reached.

| Piece | What it does |
|---|---|
| `TimerAlarmScheduler` | One `AlarmManager` alarm at the deadline, on the monotonic clock (`ELAPSED_REALTIME_WAKEUP`), allowed in Doze. |
| `TimerActionReceiver` | Receives the alarm and the notification buttons (Pause, Resume, Reset). Not exported. |
| `TimerRestoreReceiver` | `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` and the exact-alarm permission change: alarms are cleared by a reboot, so the timer is settled and the alarm set again. |
| `TimerSystemIntegration` | Started with the process. Watches the stored state and re-applies the alarm and the ongoing notification; idempotent. |
| `TimerNotifier` | Ongoing **silent** notification showing the time left in large digits: a custom layout around a system `Chronometer` counting down (it ticks without our process), fixed text while paused. And the **alert** notification (sound + vibration) when a session ends. |
| `AlertChannels` | The alert's channel. Android lets only the user change a channel's sound, so a sound picked in FocusFlow (Android's own ringtone picker) creates the next channel version with that sound, copies importance and vibration, and deletes the old one. The current sound is always read back from the system. |
| `SessionEndAlert` | Called by the repository once per newly recorded completion, whoever noticed it (screen or alarm), so there is never a double alert. |

### Platform limits (be honest with users)

* **Exact timing needs a permission.** Since Android 12 exact alarms require "Alarms & reminders"
  (`SCHEDULE_EXACT_ALARM`), which Android 14+ does not grant by default. Without it FocusFlow uses
  an inexact alarm: on time while the phone is in use, but up to several minutes late in deep idle.
  Settings shows the status and links to the permission. `USE_EXACT_ALARM` is not requested.
* **Notifications need a permission on Android 13+.** It is asked the first time a session is
  started. Without it the timer still works and records sessions, but cannot alert.
* Some manufacturers kill apps or delay alarms aggressively (battery optimisation). This is outside
  the app's control; see <https://dontkillmyapp.com>.
* The alert sound is picked in FocusFlow's Settings with Android's sound picker (default,
  silent, any alarm or notification sound). Vibration and other options stay in Android's
  channel settings, linked from the same screen.
