package com.example.guardian.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class DateKeysTest {

    private val utc = ZoneId.of("UTC")

    @Test
    fun `dateKey formats as ISO local date`() {
        // 2026-08-11T00:00:00Z
        assertEquals("2026-08-11", DateKeys.dateKey(1_786_406_400_000L, utc))
    }

    @Test
    fun `startOfToday is midnight and matches today's key`() {
        val start = DateKeys.startOfToday(utc)
        assertEquals(DateKeys.today(utc), DateKeys.dateKey(start, utc))
        // A millisecond before start belongs to the previous day.
        assertEquals(
            DateKeys.dateKey(start, utc),
            DateKeys.dateKey(start + 1, utc),
        )
    }
}
