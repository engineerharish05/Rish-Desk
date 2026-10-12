package com.rishdeskdroid.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WidgetSpanTest {
    @Test
    fun clampKeepsSizeInsideLimits() {
        val limits = SpanLimits(minW = 2, maxW = 4, minH = 1, maxH = 2, defaultW = 2, defaultH = 1)
        assertEquals(WidgetSize(4, 2), limits.clamp(WidgetSize(9, 9)))
        assertEquals(WidgetSize(2, 1), limits.clamp(WidgetSize(0, 0)))
        assertEquals(WidgetSize(2, 1), limits.clamp(null))
    }

    @Test
    fun fixedSizeWidgetIsNotResizable() {
        val limits = SpanLimits(minW = 2, maxW = 2, minH = 1, maxH = 1, defaultW = 2, defaultH = 1)
        assertFalse(limits.resizable)
    }
}
