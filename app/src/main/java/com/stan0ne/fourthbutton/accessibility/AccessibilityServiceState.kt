package com.stan0ne.fourthbutton.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager

/**
 * Detects whether this app's accessibility service is actually enabled by
 * asking the system, rather than trusting locally cached preferences.
 */
object AccessibilityServiceState {

    fun isEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? android.view.accessibility.AccessibilityManager ?: return false
        return manager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any { serviceInfo ->
            serviceInfo.resolveInfo?.serviceInfo?.packageName == context.packageName
        }
    }
}