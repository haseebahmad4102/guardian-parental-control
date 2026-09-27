package com.example.guardian.child.monitoring

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** A point-in-time view of the child device's usage, ready to upload. */
data class UsageSnapshot(
    /** package -> foreground minutes today. */
    val minutesByPackage: Map<String, Long>,
    /** package -> human-readable app label. */
    val labels: Map<String, String>,
    val foregroundApp: String?,
    val foregroundAppLabel: String?,
)

/**
 * Reads per-app foreground time from [UsageStatsManager]. Requires the special "Usage access"
 * grant (PACKAGE_USAGE_STATS); with it denied, queries return empty and callers degrade to
 * "no data" rather than crashing.
 */
@Singleton
class UsageStatsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val usageStatsManager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager: PackageManager = context.packageManager

    /**
     * Aggregates today's usage. [nowMillis] and [startOfDayMillis] are injectable for testing.
     * Packages with under a minute of foreground time, and this app itself, are dropped.
     */
    fun collect(startOfDayMillis: Long, nowMillis: Long): UsageSnapshot {
        val aggregated = runCatching {
            usageStatsManager.queryAndAggregateUsageStats(startOfDayMillis, nowMillis)
        }.getOrElse { emptyMap() }

        val minutes = LinkedHashMap<String, Long>()
        val labels = LinkedHashMap<String, String>()
        for ((pkg, stats) in aggregated) {
            if (pkg == context.packageName) continue
            val mins = stats.totalTimeInForeground / 60_000L
            if (mins < 1) continue
            minutes[pkg] = mins
            labels[pkg] = labelFor(pkg)
        }

        val foreground = currentForegroundApp(startOfDayMillis, nowMillis)
        return UsageSnapshot(
            minutesByPackage = minutes,
            labels = labels,
            foregroundApp = foreground,
            foregroundAppLabel = foreground?.let { labelFor(it) },
        )
    }

    /** The package most recently moved to the foreground within the query window, if any. */
    private fun currentForegroundApp(startMillis: Long, nowMillis: Long): String? {
        val events = runCatching {
            // Look back a few minutes so a recent switch is captured even between polls.
            usageStatsManager.queryEvents(maxOf(startMillis, nowMillis - 5 * 60_000L), nowMillis)
        }.getOrNull() ?: return null

        var latestPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND &&
                event.packageName != context.packageName
            ) {
                latestPackage = event.packageName
            }
        }
        return latestPackage
    }

    private fun labelFor(pkg: String): String = runCatching {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(pkg)
}
