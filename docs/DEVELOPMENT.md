# Development

## Requirements

* **JDK 21** (CI uses Zulu 21). The app targets Java 17 bytecode, but Robolectric needs Java 21
  to run the Android SDK 36 sandbox used by the UI tests.
* Android SDK with platform `android-37` (compileSdk 37). Android Studio installs it; on the
  command line, point `sdk.dir` in `local.properties` at your SDK (not committed).
* No emulator is needed for the default test suite: UI tests run on the JVM with Robolectric.

## Common commands

Run everything from the repository root with the bundled wrapper.

| Task | Command |
|---|---|
| Debug APK | `./gradlew assembleDebug` → `app/build/outputs/apk/debug/` |
| Install on a device | `./gradlew installDebug` |
| Unit + Robolectric UI tests | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lintDebug` |
| Format (ktlint + license headers) | `./gradlew spotlessApply` |
| Same checks as CI | `./gradlew spotlessCheck lintDebug assembleDebug testDebugUnitTest -PwarningsAsErrors=true` |

The debug build installs as `com.ignitrondev.focusflow.debug`, so it can live next to a release
build.

## Release builds

| Task | Command |
|---|---|
| Release APK (R8 minified) | `./gradlew assembleRelease` → `app/build/outputs/apk/release/` |
| Release bundle (AAB) | `./gradlew bundleRelease` → `app/build/outputs/bundle/release/` |

Signing material is **never committed**. The release build is signed when either of these is
present, and is produced as `app-release-unsigned.apk` otherwise:

* `keystore.properties` in the repository root (git-ignored), with `storeFile` (absolute path, keep the
  keystore outside the repository), `storePassword`, `keyAlias` and `keyPassword`.
* Environment variables `FOCUSFLOW_KEYSTORE_FILE`, `FOCUSFLOW_KEYSTORE_PASSWORD`,
  `FOCUSFLOW_KEY_ALIAS` and `FOCUSFLOW_KEY_PASSWORD` (used by CI in Phase 7).

Only the maintainers hold the release key. Contributors build debug, or sign release builds with
their own key (they cannot be installed over the official release).

Official release signing certificate (SHA-256), to verify downloaded APKs with
`apksigner verify --print-certs`:

```
5a:ad:b1:31:3d:4a:ad:9a:c1:d0:2a:df:ef:5d:03:e6:79:de:57:aa:62:66:93:70:db:06:b0:83:aa:94:b1:ee
```

## Conventions

* New files start with the Apache-2.0 header for `IgnitronDev` (`spotless/copyright.kt`);
  `spotlessApply` adds it. Files adapted from `android/compose-samples` keep their original header.
* All user-facing text lives in `res/values/strings.xml`.
* Screens are split into a stateless `XxxScreen(uiState, callbacks)` composable (previewable,
  testable) and a small route function in the same feature package that wires the ViewModel.
* `domain/` must not import Android or framework classes.
* Inject dispatchers with `@Dispatcher(FocusFlowDispatcher.IO)`; never hard-code
  `Dispatchers.IO` in classes that need tests.

## Notes

* Robolectric's SDK 36 shadows need `--add-opens=java.base/jdk.internal.access=ALL-UNNAMED`
  on JDK 21; this is configured in `app/build.gradle.kts`.
* `org.gradle.jvmargs=-Xmx2048m` keeps builds usable on 8 GB machines. CI uses the shared
  `.github/ci-gradle.properties` (more memory, no daemon, warnings as errors).
* Lint ignores test sources (`lint { ignoreTestSources = true }`): with AGP 9.3, analysing the
  Hilt test classes can race with KSP and crash lint.
* Lint currently reports only "newer version available" warnings. FocusFlow's versions are kept
  aligned with `android/compose-samples` and are bumped together, deliberately.
