# Overlay

## Window layer

Every floating view uses `WindowManager.TYPE_ACCESSIBILITY_OVERLAY` (API 26+)
because the app already holds the Accessibility permission - this layer sits
above a normal app but below the system keyguard, which is desirable.

`OverlayWindowManager` centralizes:

- `addView` (returns success; never double-adds; guards `BadTokenException`)
- `updateViewLayout`
- `removeView` / `removeViewImmediate`
- `removeAll()` for teardown on service destroy

## Floating button

- A small circular `ImageView` whose centre is tracked.
- Position is persisted as a centre in `SharedPreferences`.
- On start the saved centre is clamped to the visible display; the same clamp
  logic (`OverlayPositionCalculator`) is unit tested.

## Touch / tap / drag

`FloatingButtonManager` distinguishes tap from drag using the platform touch
slop:

- `ACTION_DOWN`: record raw + window positions.
- `ACTION_MOVE`: if moved beyond `scaledTouchSlop`, mark as drag and move the
  window.
- `ACTION_UP`: if it was a drag, persist the new centre and notify the drag
  listener; otherwise treat as a tap and open the menu.

On orientation change `onDisplaySizeChanged()` clamps the button back inside
the new bounds so it is never lost off-screen.

## Floting menu

`FloatingMenuManager` is one transparent full-screen window. The menu card is
positioned next to the button:

- Above the button, unless there is no room, in which case below.
- Horizontally aligned to the button and clamped to the screen edge.

Tapping outside the card dismisses it; tapping inside fires the row's action.
Rendering resolves dark/light system theme and builds one row per
`ActionItem`. Open/close use fast fade + scale (<200 ms).

## Outside-tap / Back

Touching the transparent surrounding area calls `dismiss()`. The accessibility
service also guards so that when the menu is open, pressing the soft Back is
not needed - the outside-tap covers the primary close gesture.