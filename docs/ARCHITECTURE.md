# Architecture

A single `:app` module, organised by layer. Data flows one way: the UI sends actions down and
observes state coming back up.

```
feature/        Compose screens + ViewModels (timer, statistics, history, settings)
   │  observes StateFlow, sends actions
domain/         Pure Kotlin: models, TimerEngine, statistics, repository interfaces
   │  implemented by
data/           Room (timer_state, focus_sessions), DataStore (settings), repositories
platform/       Android integration: AlarmManager, notifications, broadcast receivers
core/           Design system, time utilities, dispatchers, DataStore setup
navigation/     Top-level destinations and the adaptive NavigationSuiteScaffold
```

## Rules

* `domain/` imports no Android or framework classes, so the engine and the statistics run as plain
  JVM unit tests.
* Screens are stateless `XxxScreen(uiState, callbacks)` composables; a small route function wires
  the ViewModel. Previews and UI tests use the stateless version.
* ViewModels expose one `StateFlow<XxxUiState>`, built with `stateIn(WhileSubscribed(5_000))`.
* Time comes from the injectable `TimeSource`; nothing reads the system clock directly.
* Dependency injection with Hilt. Tests replace modules with `@TestInstallIn` (in-memory Room, fake
  clock, fake settings).

## The timer, end to end

1. The user taps Start → `TimerViewModel` → `TimerRepository.start()`.
2. The repository, in one Room transaction, settles the stored state, applies the action with
   `TimerEngine` and writes it back (see [TIMER_ENGINE.md](TIMER_ENGINE.md)).
3. `TimerSystemIntegration` (application scope) observes the new state and schedules the deadline
   alarm and the countdown notification.
4. The screen observes the same state and re-derives the remaining time once per second.
5. At the deadline, the alarm (or the open screen, whichever is first) calls `refresh()`; the
   session is recorded once and `SessionEndAlert` posts the alert.

## Storage

* Room: timer state and session history ([DATABASE.md](DATABASE.md)).
* DataStore Preferences: appearance and timer settings.
* Nothing leaves the device; the app has no network permission. Sharing a statistics card writes a
  PNG to the app cache and hands it, through `FileProvider` and Android's share sheet, to the app
  the user picks; only that app gets read access to that one file.
