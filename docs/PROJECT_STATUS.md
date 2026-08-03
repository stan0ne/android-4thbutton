# PROJECT STATUS — 4thButton

> An Android app that replaces a broken physical power button with a floating,
> always-available software menu: **Screen Lock, Power Menu, Screenshot,
> Flashlight**.

## Current status

**COMPLETE - buildable, testable and verified on Android 12 (API 31).**

- Green build: `./gradlew assembleDebug`
- Green static analysis: `./gradlew lintDebug` (0 errors)
- Unit tests pass: `./gradlew testDebugUnitTest`
- Manual P0 suite PASSED on the `emu_api` (API 31) emulator

## What works (verified on Android 12)

- Installs & launches on Android 12.
- Accessibility service connects (`AssistivePower/SERVICE` logs).
- Floating draggable button appears over other apps; a drag persists the new
  position (SharedPreferences) across restarts.
- Tap opens a compact floating menu with **Screen Lock, Screenshot, Power
  Menu, Flashlight**.
- **Screen Lock**: device wakefulness goes Awake → Asleep.
- **Power Menu**: `GLOBAL_ACTION_POWER_DIALOG` returns `true`.
- **Screenshot**: default 3 s countdown runs → captures → writes PNG to
  MediaStore (`Pictures/AssistivePower/Screenshot_2026-08-03_00-21-55.png`).
  Button/menu are hidden for the capture, then restored.
- **Flashlight**: toggles camera `torch on` / `torch off`.

## Data / privacy posture

- No internet permission; no analytics; offline.
- Accessibility service does NOT read window content
  (`canRetrieveWindowContent=false`), stores no screen data, and screenshots
  are only ever taken on the user's explicit tap and saved locally.

## Honest limitations surfaced

| Area | Limitation | Decision |
|------|-----------|----------|
| Reboot | No non-root public API on Android 12 | Not faked. Reach reboot via the real system **Power Menu** (`GLOBAL_ACTION_POWER_DIALOG`). No `runtime.exec/reboot/su`. |
| Flashlight | Needs a camera flash | Menu hides the action or reports unavailable when `hasFlash()` is false; never crashes. |
| Screenshot | Public API min is API 30 | Show the action regardless; on older devices a clear message is shown instead of a broken capture. |

## Build

Android 12 (API 31) recommended; minSdk 28; compile/target SDK 35.

Build output: `app/build/outputs/apk/debug/app-debug.apk`

## Remaining / optional

- Retest the Power Menu dialog's actual dialog UI and auto-capture flows on a
  physical Android 12 device (the emulator returns `true` for
  `GLOBAL_ACTION_POWER_DIALOG` but may not render the full system UI in
  headless mode).
- Screenshot delay 3/5/10 s individually may be re-verified on hardware.
- A future build could add a custom action-order setting.

## How to build & test

```
./gradlew assembleDebug
./gradlew lintDebug
./gradlew testDebugUnitTest
```

See `docs/`, `README.md` for architecture, accessibility, overlay, actions,
screenshot and settings details.