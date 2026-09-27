package com.example.guardian.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Produces the `yyyy-MM-dd` keys used as `usage/{date}` document ids. Kept here so the child
 * (which writes) and the parent (which reads a date range) agree on the format and time zone.
 */
object DateKeys {
    /** Local-date key for [epochMillis] in [zone] (defaults to the device's zone). */
    fun dateKey(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toString()

    fun today(zone: ZoneId = ZoneId.systemDefault()): String = LocalDate.now(zone).toString()

    /** The [count] most recent local-date keys, today first (e.g. count=7 → today .. 6 days ago). */
    fun recentDateKeys(count: Int, zone: ZoneId = ZoneId.systemDefault()): List<String> {
        val today = LocalDate.now(zone)
        return (0 until count).map { today.minusDays(it.toLong()).toString() }
    }

    /** Start-of-today epoch millis in [zone] — the query window start for "today's usage". */
    fun startOfToday(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
}
