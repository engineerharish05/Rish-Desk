package com.rishdeskdroid.app.data

/** Outcome of checking whether a previously discovered launcher activity can still be opened. */
enum class LaunchTargetState { AVAILABLE, MISSING, DISABLED }

/** Pure launch-readiness decision so stale and disabled targets can be regression-tested. */
fun launchTargetState(
    activityFound: Boolean,
    activityEnabled: Boolean,
    applicationEnabled: Boolean,
): LaunchTargetState = when {
    !activityFound -> LaunchTargetState.MISSING
    !activityEnabled || !applicationEnabled -> LaunchTargetState.DISABLED
    else -> LaunchTargetState.AVAILABLE
}
