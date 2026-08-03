# Accessibility

## Why a service

Screen lock, power dialog and programmatic screenshots are system functions.
The public, official way for an app to reach them is an `AccessibilityService`
(it is designed and documented for general-purpose assistive tools).

## Minimal capability footprint

`res/xml/accessibility_service_config.xml`:

```xml
android:accessibilityEventTypes="typeWindowStateChanged"
android:accessibilityFeedbackType="feedbackGeneric"
android:accessibilityFlags="flagIncludeNotImportantViews|flagReportViewIds"
android:canPerformGestures="false"
android:canRetrieveWindowContent="false"
android:canTakeScreenshot="true"
```

- `canRetrieveWindowContent="false"` - we never read the UI hierarchy.
- `canPerformGestures="false"` - we do not dispatch gestures.
- `eventTypes="typeWindowStateChanged"` - minimal; events are ignored anyway
  (`onAccessibilityEvent` is intentionally empty).
- `canTakeScreenshot="true"` - required so `takeScreenshot()` is permitted.

The manifest service declaration uses
`android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`.

### Manifest

```xml
<service
    android:name=".accessibility.AssistiveAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

## Privacy / security

- The service reads no window content and stores no screen data.
- It performs only the exact action the user taps.
- No sensitive detail is logged in production paths.