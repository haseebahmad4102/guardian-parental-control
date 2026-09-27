package com.example.guardian.parent.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun `formats minutes into compact durations`() {
        assertEquals("0m", formatMinutes(0))
        assertEquals("0m", formatMinutes(-5))
        assertEquals("5m", formatMinutes(5))
        assertEquals("59m", formatMinutes(59))
        assertEquals("1h", formatMinutes(60))
        assertEquals("1h 15m", formatMinutes(75))
        assertEquals("2h", formatMinutes(120))
        assertEquals("25h 1m", formatMinutes(1501))
    }
}
