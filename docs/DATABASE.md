# Database

Room database `focusflow.db`, version **1**. Schemas are exported to `app/schemas/` and committed;
they are the reference for migrations.

## Rules

* Every schema change bumps the version and ships a migration (or auto-migration) with a test.
* **No destructive migrations**: `fallbackToDestructiveMigration` would delete the user's history.
* Writes that must stay consistent (timer state + session history) run in one transaction.
* `DatabaseSchemaTest` builds a database from each exported schema and opens it with Room, which
  fails if the entities changed without a version bump. Every migration gets a test there:
  create the old version from its JSON, migrate, and check the data survived.

## Settings (DataStore, not Room)

`user_preferences.preferences_pb`: theme, dynamic color, and the timer settings (`focus_minutes`,
`short_break_minutes`, `long_break_minutes`, `long_break_interval`, `auto_start_breaks`,
`auto_start_focus`). Out-of-range values are clamped when read.

## `timer_state`

A single row (`id = 1`) holding the timer. See [TIMER_ENGINE.md](TIMER_ENGINE.md).

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | Always 1 |
| `run_id` | TEXT | Id of the current session run (UUID) |
| `session_type` | TEXT | `Focus`, `ShortBreak`, `LongBreak` |
| `status` | TEXT | `Idle`, `Running`, `Paused` |
| `planned_duration_millis` | INTEGER | Full length of the session |
| `remaining_millis` | INTEGER | Time left; while running, at the anchor |
| `anchor_elapsed_realtime_millis` | INTEGER? | Monotonic clock at the anchor (running only) |
| `anchor_epoch_millis` | INTEGER? | Wall clock at the anchor (running only) |
| `anchor_boot_count` | INTEGER? | Boot count at the anchor, when available |
| `started_at_epoch_millis` | INTEGER? | First start of this run |
| `completed_focus_in_cycle` | INTEGER | Focus sessions since the last long break |
| `updated_at_epoch_millis` | INTEGER | Last write |

The deadline is derived (`anchor + remaining`) rather than stored, so it can never disagree with
the other columns.

## `focus_sessions`

Finished sessions (completed or interrupted). Rows are written once and never updated.

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | The timer run id: one row per run, duplicates are ignored |
| `session_type` | TEXT | `Focus`, `ShortBreak`, `LongBreak` |
| `started_at` | INTEGER | Epoch millis |
| `ended_at` | INTEGER | Epoch millis; the real deadline for completed sessions |
| `planned_duration_seconds` | INTEGER | |
| `actual_duration_seconds` | INTEGER | Running time, excluding pauses |
| `completed` | INTEGER (bool) | `0` = reset or skipped |

Indexes: `ended_at` (history by date) and `(session_type, completed, ended_at)` (daily and weekly
focus totals).
