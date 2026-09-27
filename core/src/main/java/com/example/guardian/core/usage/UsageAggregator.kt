package com.example.guardian.core.usage

import com.example.guardian.core.model.UsageRecord

/** One app's rolled-up foreground time over the selected window. */
data class AppUsage(
    val pkg: String,
    val label: String,
    val minutes: Long,
)

/** Result of rolling up usage records, sorted by descending minutes. */
data class AggregatedUsage(
    val perApp: List<AppUsage>,
    val totalMinutes: Long,
) {
    val isEmpty: Boolean get() = perApp.isEmpty()
}

/**
 * Rolls up daily [UsageRecord]s into a per-app total, restricted to the given [dates]. Kept in
 * core so it's pure and unit-testable — the parent's daily view passes a single date, the weekly
 * view passes seven.
 */
object UsageAggregator {

    fun aggregate(records: List<UsageRecord>, dates: Set<String>): AggregatedUsage {
        val minutes = HashMap<String, Long>()
        val labels = HashMap<String, String>()
        for (record in records) {
            if (record.date !in dates) continue
            for ((pkg, mins) in record.apps) {
                minutes[pkg] = (minutes[pkg] ?: 0L) + mins
                record.labels[pkg]?.takeIf { it.isNotBlank() }?.let { labels[pkg] = it }
            }
        }
        val perApp = minutes.entries
            .map { (pkg, mins) -> AppUsage(pkg, labels[pkg] ?: pkg, mins) }
            .sortedWith(compareByDescending<AppUsage> { it.minutes }.thenBy { it.label })
        return AggregatedUsage(perApp = perApp, totalMinutes = perApp.sumOf { it.minutes })
    }
}
