package com.example.guardian.core.model

/**
 * One day of per-app foreground usage. Firestore path: `devices/{deviceId}/usage/{date}`,
 * where `date` is an ISO local date, e.g. `2026-08-11`.
 *
 * [apps] maps an app package name to minutes of foreground time that day.
 */
data class UsageRecord(
    val date: String = "",
    val apps: Map<String, Long> = emptyMap(),
    /**
     * Human-readable labels for the packages in [apps] (package -> app name), filled in by the
     * child. The parent can't resolve another device's app labels, so they travel with the data.
     */
    val labels: Map<String, String> = emptyMap(),
    val updatedAt: Long = 0L,
) {
    /** Total foreground minutes across all apps for the day. */
    val totalMinutes: Long get() = apps.values.sum()

    fun labelFor(pkg: String): String = labels[pkg]?.takeIf { it.isNotBlank() } ?: pkg
}
