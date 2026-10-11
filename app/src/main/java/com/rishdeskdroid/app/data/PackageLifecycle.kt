package com.rishdeskdroid.app.data

/**
 * Identifies package lifecycle broadcasts that can change the launchable-app list.
 * Kept pure so action handling can be regression-tested without an Android device.
 */
fun shouldReloadAppsForPackageAction(action: String?): Boolean = when (action) {
    "android.intent.action.PACKAGE_ADDED",
    "android.intent.action.PACKAGE_REMOVED",
    "android.intent.action.PACKAGE_CHANGED",
    "android.intent.action.PACKAGE_REPLACED" -> true
    else -> false
}
