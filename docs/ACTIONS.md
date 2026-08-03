# Actions

## Model

`AssistiveAction` is an extensible enum (id, icon, default order). The menu
always renders `ActionRepository.visibleActions()`.

## Currently implemented

| Action id      | Title       | Implementation                                  |
|----------------|-------------|-------------------------------------------------|
| `screen_lock`  | Screen Lock | `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` (API 28+) |
| `screenshot`   | Screenshot  | `AccessibilityService.takeScreenshot(...)` (API 30+) |
| `power_menu`   | Power Menu  | `performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)` |
| `flashlight`   | Flashlight  | `CameraManager.setTorchMode(cameraId, true/false)` |
| `reboot`       | Reboot      | not a real action (see below)                    |

Every `performGlobalAction` result is checked: `false` → meaningful toast.

## Availability

`ActionRepository.isAvailable()`:

- Screen Lock / Screenshot / Power Menu: always true.
- Flashlight: true only when a rear camera with a flash is detected
  (`FLASH_INFO_AVAILABLE` + `LENS_FACING_BACK`).
- Reboot: false on normal devices.

## Reboot (honest limitation)

There is no non-root public API to reboot Android 12. The app does **not**
fake it, does **not** shell out to `runtime.exec("reboot")` or `su`. Instead
the user reaches reboot through the real system **Power Menu**
(`GLOBAL_ACTION_POWER_DIALOG`). The ARCHITECTED capability field exists so a
future device-owner/system build could enable direct reboot.

## Settings / configuration

- `AppPreferences.isActionEnabled(id)` - whether the action shows in the menu.
- Filtered by capability too, so e.g. a no-flash device does not show
  Flashlight.