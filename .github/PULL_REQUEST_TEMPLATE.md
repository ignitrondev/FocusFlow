## What and why

<!-- What does this change, and why is it needed? Link the issue if there is one. -->

## How it was tested

<!-- Tests added or updated; manual checks on a device (model, Android version). -->

## Checklist

- [ ] `./gradlew spotlessCheck lintDebug assembleDebug testDebugUnitTest -PwarningsAsErrors=true` passes
- [ ] User-facing text is in `strings.xml`
- [ ] Screenshots in light and dark theme for UI changes
- [ ] No network access, analytics or tracking added
- [ ] Room schema changes: version bumped, migration and test added, new schema JSON committed
