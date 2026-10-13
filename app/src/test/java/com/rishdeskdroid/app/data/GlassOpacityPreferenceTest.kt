package com.rishdeskdroid.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassOpacityPreferenceTest {

    @Test
    fun valuesInsideSupportedRangeArePreserved() {
        assertEquals(0f, sanitizeGlassOpacity(0f), 0f)
        assertEquals(0.001f, sanitizeGlassOpacity(0.001f), 0f)
        assertEquals(0.35f, sanitizeGlassOpacity(0.35f), 0f)
        assertEquals(0.999f, sanitizeGlassOpacity(0.999f), 0f)
        assertEquals(1f, sanitizeGlassOpacity(1f), 0f)
    }

    @Test
    fun outOfRangeValuesAreClamped() {
        assertEquals(0f, sanitizeGlassOpacity(-0.25f), 0f)
        assertEquals(1f, sanitizeGlassOpacity(1.75f), 0f)
    }

    @Test
    fun nonFiniteValuesFallBackToDefaultOpacity() {
        assertEquals(0.6f, sanitizeGlassOpacity(Float.NaN), 0f)
        assertEquals(0.6f, sanitizeGlassOpacity(Float.POSITIVE_INFINITY), 0f)
        assertEquals(0.6f, sanitizeGlassOpacity(Float.NEGATIVE_INFINITY), 0f)
    }
}
