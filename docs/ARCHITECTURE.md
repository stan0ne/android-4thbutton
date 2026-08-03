# Architecture

## Overview

```
          MAIN ACTIVITY (companion config UI)
         - accessibility status
         - enable/disable actions
         - screenshot delay
        |
        | uses AppPreferences (SharedPreferences)
        |
ACCESSIBILITY SERVICE (core)
   ├── OverlayWindowManager   (add/update/remove windows, safe teardown)
   ├── FloatingButtonManager  (touch, drag, position persistence)
   ├── FloatingMenuManager    (compact menu, animation, outside-tap close)
   ├── CountdownOverlay       (screenshot countdown + cancel)
   ├── ActionRepository       (which actions appear, in which order)
   ├── FlashlightController   (CameraManager torch)
   └── ScreenshotCoordination (delay -> hide UI -> takeScreenshot -> MediaStore)
```

The service is the product. The Activity is only a configuration companion and
is not required for daily use.

## Responsibility separation

- **accessibility/** - the service and real accessibility-state detection.
- **overlay/** - all WindowManager housekeeping; no business logic.
- **actions/** - what to show and how the torch is toggled.
- **screenshot/** - capture orchestration, pure filename/scheduler helpers.
- **settings/** - typed persistence over SharedPreferences.

Pure logic (OverlayPositionCalculator, ScreenshotFileNameGenerator,
ScreenshotDelayScheduler) is dependency-free so it is unit-testable.

## Window management

All overlay views are created with `TYPE_ACCESSIBILITY_OVERLAY` (API 26+, min
is 28) and non-focusable flags. Add/update/remove is wrapped in
`OverlayWindowManager` so `BadTokenException` / `IllegalStateException` /
double-add are handled centrally and everything is removed on service destroy.

## State vs. capabilities

`AccessibilityServiceState.isEnabled()` queries the system rather than trusting
the local preference. The service advertises the screenshot capability via
`android:canTakeScreenshot="true"` in `res/xml/accessibility_service_config.xml`.