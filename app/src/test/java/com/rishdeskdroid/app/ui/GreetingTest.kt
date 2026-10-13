package com.rishdeskdroid.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test
    fun greetingFollowsTheHourOfDay() {
        assertEquals("Good night", greetingText(0))
        assertEquals("Good night", greetingText(4))
        assertEquals("Good morning", greetingText(5))
        assertEquals("Good morning", greetingText(11))
        assertEquals("Good afternoon", greetingText(12))
        assertEquals("Good afternoon", greetingText(16))
        assertEquals("Good evening", greetingText(17))
        assertEquals("Good evening", greetingText(20))
        assertEquals("Good night", greetingText(21))
        assertEquals("Good night", greetingText(23))
    }
}
