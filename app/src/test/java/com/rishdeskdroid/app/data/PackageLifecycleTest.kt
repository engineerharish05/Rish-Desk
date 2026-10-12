package com.rishdeskdroid.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageLifecycleTest {

    @Test
    fun packageInstallRemovalChangeAndReplacementTriggerRefresh() {
        val lifecycleActions = listOf(
            "android.intent.action.PACKAGE_ADDED",
            "android.intent.action.PACKAGE_REMOVED",
            "android.intent.action.PACKAGE_CHANGED",
            "android.intent.action.PACKAGE_REPLACED",
        )

        lifecycleActions.forEach { action ->
            assertTrue("$action should refresh the app list", shouldReloadAppsForPackageAction(action))
        }
    }

    @Test
    fun unrelatedOrMissingActionsDoNotTriggerRefresh() {
        assertFalse(shouldReloadAppsForPackageAction(null))
        assertFalse(shouldReloadAppsForPackageAction(""))
        assertFalse(shouldReloadAppsForPackageAction("android.intent.action.BOOT_COMPLETED"))
        assertFalse(shouldReloadAppsForPackageAction("android.intent.action.PACKAGE_DATA_CLEARED"))
    }
}
