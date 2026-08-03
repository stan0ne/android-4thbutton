# Settings

## Persistence

All config lives in `AppPreferences`, a typed wrapper over a single
`SharedPreferences` (`AppPrefs`). No other storage is used.

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `button_center_x` | Float | clamped baseline | Floating button centre X |
| `button_center_y` | Float | clamped baseline | Floating button centre Y |
| `screenshot_delay_ms` | Long | 2500 | Screenshot delay in ms |
| `flashlight_enabled` | Bool | true | Flashlight row visible |
| `screenshot_enabled` | Bool | true | Screenshot row visible |
| `power_menu_enabled` | Bool | true | Power Menu row visible |
| `screen_lock_enabled` | Bool | true | Screen Lock row visible |
| `first_run_done` | Bool | false | Has on-boarded |

## Migration

Stored values are validated/clamped on read, so a stale or corrupt prefs value
can never crash or push the button off-screen.

## Action order

The menu order is fixed to: Screen Lock, Screenshot, Power Menu, Flashlight. A
future setting could persist a custom order.