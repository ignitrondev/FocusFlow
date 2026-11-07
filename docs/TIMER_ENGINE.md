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

## Not in Phase 2

* **Notifications while the app is closed.** A finished session is recorded the next time the app
  opens, with the correct end time, but the user is not alerted yet. Phase 3 adds an alarm
  (`AlarmManager`; exact only when the user allows exact alarms) and a countdown notification.
* Configurable durations, auto-start switches and sound/vibration settings (Phase 5). The engine
  already supports them through `TimerConfig`.
