package com.rishdeskdroid.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchReadinessTest {

    @Test
    fun missingActivityIsReportedAsMissing() {
        assertEquals(
            LaunchTargetState.MISSING,
            launchTargetState(activityFound = false, activityEnabled = false, applicationEnabled = false),
        )
    }

    @Test
    fun disabledActivityOrApplicationIsNotLaunchable() {
        assertEquals(
            LaunchTargetState.DISABLED,
            launchTargetState(activityFound = true, activityEnabled = false, applicationEnabled = true),
        )
        assertEquals(
            LaunchTargetState.DISABLED,
            launchTargetState(activityFound = true, activityEnabled = true, applicationEnabled = false),
        )
    }

    @Test
    fun enabledActivityInEnabledApplicationIsLaunchable() {
        assertEquals(
            LaunchTargetState.AVAILABLE,
            launchTargetState(activityFound = true, activityEnabled = true, applicationEnabled = true),
        )
    }
}
