package com.example.guardian.core.model

/** A launchable app on the child device. */
data class InstalledApp(
    val pkg: String = "",
    val label: String = "",
)

/**
 * The child's list of launchable apps. Firestore path: `devices/{deviceId}/meta/installedApps`.
 * The parent reads this so limits/blocks can target ANY installed app, not only ones already used.
 */
data class InstalledAppsDoc(
    val apps: List<InstalledApp> = emptyList(),
    val updatedAt: Long = 0L,
)
