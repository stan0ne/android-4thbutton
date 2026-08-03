# 4thButton

Replace your broken physical power button with software controls.

## Purpose

If the physical Power button on your Android phone is broken, you lose quick
access to the lock screen, screenshots, the power menu and the flashlight.
**4thButton** puts those system actions back in your hand as a small,
always-available floating button that works over any app.

## Features

- Floating, draggable button that sits over all applications
- Tap the button to open a compact floating menu
- **Screen Lock** - locks the screen (`GLOBAL_ACTION_LOCK_SCREEN`)
- **Power Menu** - opens Android's real system power dialog (`GLOBAL_ACTION_POWER_DIALOG`)
- **Screenshot** - one-tap capture, with optional delay + countdown
- **Flashlight** - toggle via `CameraManager.setTorchMode()`
- Action enable/disable from the settings screen
- Screenshot delay selector (Immediate / 3s / 5s / 10s)
- Dark/light theme-aware floating UI
- No account, no ads, no analytics, no internet

## Requirements

- Android 12 (API 31) recommended, min SDK 28
- Any phone with a broken power button

## Android Version

- `minSdk = 28` (Android 9)
- `compileSdk / targetSdk = 35`
- Primary test platform: **Android 12 / API 31**

## Accessibility Permission

This app is a general-purpose assistive tool. The floating overlay, screen
lock, screenshot and power-menu functionality are implemented as an Android
**AccessibilityService**. That is why the app requires the Accessibility
permission - it is the official, public mechanism for these features.

The service is strictly minimal by design:

- it never reads window content (`canRetrieveWindowContent=false`)
- it registers no window event handlers that process content
- it stores no screen data and never logs what is on screen
- screenshots are only ever taken when you explicitly tap the Screenshot
  action, and are saved only to your local gallery via MediaStore

## Installation

1. Open the project in Android Studio.
2. Build the debug APK or run on device.
3. Install and launch. On first run the app asks you to enable Accessibility.
4. Tap **Enable Accessibility** to open system settings and switch the
   service on. Return to the app - it confirms the live status.

No storage permission is required on Android 10+ (scoped storage + MediaStore).

## Usage

After setup a round button appears at the edge of the screen.

- **Drag** it anywhere; its position is remembered between restarts.
- **Tap** it to open the menu.
- Tap an action. The menu closes after the action fires.

## Screenshot

- Tap **Screenshot** in the menu.
- If a delay is configured a small countdown overlay appears (with Cancel).
- The floating button and menu are hidden just before capture so they do not
  appear in the picture.
- The PNG is saved to `Pictures/AssistivePower/` (MediaStore) and a short
  "Screenshot saved" toast is shown.

## Screen Lock

Tap **Screen Lock** → the device locks immediately. No physical Power button
is needed.

## Power Menu

Tap **Power Menu** → Android's own system power dialog opens (Restart,
Power off, etc.). This app never recreates a fake power menu.

## Flashlight

Tap **Flashlight** toggles the torch. The menu shows "Flashlight" only when a
camera with a flash is present; otherwise the action reports unavailable.

## Reboot limitation

A normal, non-root third-party app **cannot** trigger a direct device reboot on
Android 12 - there is no public API for it. 4thButton therefore does not
pretend to reboot. Instead the user reaches reboot through the system **Power
Menu**, which this app opens via `GLOBAL_ACTION_POWER_DIALOG`. No root, shell,
or hidden API is ever used.

## Privacy

- Collects no data.
- No analytics SDK.
- No internet permission (offline utility).
- Never reads or stores window content.
- Screenshots are only saved when explicitly requested by the user.

## Architecture

```
app/src/main/java/com/stan0ne/fourthbutton
├── accessibility/  AccessibilityService + state detection
├── overlay/        FloatingButton / FloatingMenu / Countdown / Window wrapper
├── actions/        ActionRepository + FlashlightController
├── screenshot/     Capture orchestration, MediaStore storage, filename utils
├── settings/       AppPreferences (SharedPreferences)
└── util/           Logging + theme helpers
```

See `docs/` for detailed documentation.

## Testing

Unit tests:

```
./gradlew testDebugUnitTest
```

Static analysis:

```
./gradlew lintDebug
```

Build:

```
./gradlew assembleDebug
```

The full feature set (accessibility service, floating overlay, screen lock,
power menu, screenshot, flashlight) requires a real or emulated Android 12
device. See `docs/TESTING.md`.

## Known limitations

- Direct reboot is not available on non-root devices (falls back to Power Menu).
- Screenshot requires Android 11+ (API 30+), which is the public API's minimum.
- Flashlight requires a device with a camera flash.