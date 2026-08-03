# Testing

## Unit tests (JVM, dependency-free)

```
./gradlew testDebugUnitTest
```

| Class | Verifies |
|-------|----------|
| `OverlayPositionCalculatorTest` | clamping a saved centre back into the visible display |
| `ScreenshotFileNameGeneratorTest` | filename format/locale/time |
| `ScreenshotDelaySchedulerTest` | coroutine delay ticking + cancel |

All green.

## Static analysis

```
./gradlew lintDebug
```

Expected: 0 errors (warnings, if any, are acceptable and documented).

## Build

```
./gradlew assembleDebug
```

## Manual feature matrix (Android 12 / emulator `emu_api31`)

Latest run P0 status:

| # | Test | Result |
|---|------|--------|
| 1 | APK installs & launches | PASS |
| 2 | Accessibility service enables & connects | PASS |
| 3 | Floating button shown over other apps | PASS |
| 4 | Button drag + position persists after drag | PASS |
| 5 | Menu opens with 4 items | PASS |
| 6 | Screen Lock locks device | PASS |
| 7 | Power Menu (`GLOBAL_ACTION_POWER_DIALOG` returns true) | PASS |
| 8 | Screenshot captures + countdown + saves PNG to MediaStore | PASS |
| 9 | Flashlight on/off via camera flash | PASS |

P1 / P2 (retest on real hardware):

- Screenshot delay 3/5/10 s each produce correct countdown & save.
- Rotate device: button stays on screen.
- Accessibility toggled off: button disappears, app relaunching re-enables.
- Service reconnects after force-stop.
- Power Menu actually renders the dialog on a real device (emulator returns `true` but may not draw the system UI).