package com.example.guardian.core.usage

import com.example.guardian.core.model.UsageRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageAggregatorTest {

    private val monday = UsageRecord(
        date = "2026-08-10",
        apps = mapOf("com.tiktok" to 60L, "com.chrome" to 30L),
        labels = mapOf("com.tiktok" to "TikTok", "com.chrome" to "Chrome"),
    )
    private val tuesday = UsageRecord(
        date = "2026-08-11",
        apps = mapOf("com.tiktok" to 20L, "com.wa" to 15L),
        labels = mapOf("com.tiktok" to "TikTok"),
    )

    @Test
    fun `single-date aggregation returns that day only, sorted desc`() {
        val result = UsageAggregator.aggregate(listOf(monday, tuesday), setOf("2026-08-11"))
        assertEquals(35L, result.totalMinutes)
        assertEquals("com.tiktok", result.perApp.first().pkg)
        assertEquals(20L, result.perApp.first().minutes)
    }

    @Test
    fun `multi-date aggregation sums across days and merges labels`() {
        val result = UsageAggregator.aggregate(
            listOf(monday, tuesday),
            setOf("2026-08-10", "2026-08-11"),
        )
        assertEquals(60L + 30L + 20L + 15L, result.totalMinutes)
        val tiktok = result.perApp.first { it.pkg == "com.tiktok" }
        assertEquals(80L, tiktok.minutes)
        assertEquals("TikTok", tiktok.label)
        // A package whose label never arrived falls back to the package name.
        assertEquals("com.wa", result.perApp.first { it.pkg == "com.wa" }.label)
    }

    @Test
    fun `dates outside the window are ignored`() {
        val result = UsageAggregator.aggregate(listOf(monday, tuesday), setOf("2020-01-01"))
        assertTrue(result.isEmpty)
        assertEquals(0L, result.totalMinutes)
    }
}
