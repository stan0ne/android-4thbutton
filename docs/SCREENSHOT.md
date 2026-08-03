# Screenshot

## Capture

Implemented with the public `AccessibilityService.takeScreenshot(int,
Executor, callback)` API (Android 11+, API 30+).

Flow:

1. Tap Screenshot.
2. The floating button and menu are hidden (`buttonManager.hide()`).
3. If a delay is set, a `CountdownOverlay` runs; each whole second updates the
   number and a Cancel button aborts with `ScreenshotDelayScheduler.cancel()`.
4. On completion the overlay is hidden and after a tiny pause
   `takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, callback)` is called.
5. On success the returned `HardwareBuffer` is wrapped into a `Bitmap`
   (`Bitmap.wrapHardwareBuffer`) and written through MediaStore.

## Storage (no permission needed)

The PNG goes to `Pictures/AssistivePower/` via the scoped-storage MediaStore
API (`MediaStore.Images.Media.EXTERNAL_CONTENT_URI` with `RELATIVE_PATH` and
`IS_PENDING`). No storage permission is requested on Android 10+.

Filename: `Screenshot_2026-08-03_00-21-55.png` (see
`ScreenshotFileNameGenerator`).

## Keeping UI out of the shot

The floating button and any menu are removed before capture and re-shown in the
callback, so they do not appear in the saved image.

## Errors

- No `HardwareBuffer` → "Screenshot failed".
- MediaStore write failure → "Screenshot failed to save." and the pending
  entry is deleted.
- Callback `onFailure(code)` → logged and a toast is shown.
- The bitmap is `recycle()`d and the `HardwareBuffer` closed in `finally` so
  no pixel memory leaks.