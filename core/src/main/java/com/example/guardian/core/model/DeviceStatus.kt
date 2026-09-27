package com.example.guardian.core.model

/**
 * Live health/status of a child device. Firestore path: `devices/{deviceId}/status/current`.
 *
 * Lets the parent see, at a glance, whether monitoring is actually running and which special
 * permissions the child has granted — so the dashboard can surface graceful-degradation gaps
 * (e.g. "usage access not granted, screen-time data is unavailable").
 */
data class DeviceStatus(
    val monitoringActive: Boolean = false,
    val foregroundApp: String = "",
    val foregroundAppLabel: String = "",
    val usageAccessGranted: Boolean = false,
    val notificationAccessGranted: Boolean = false,
    /** [OwnerMode] name: DEVICE_OWNER unlocks silent app-hiding + uninstall protection. */
    val ownerMode: String = OwnerMode.STANDARD.name,
    /** True when the child app has blocked its own uninstall (Device Owner only). */
    val uninstallBlocked: Boolean = false,
    val updatedAt: Long = 0L,
) {
    val mode: OwnerMode get() = OwnerMode.fromValue(ownerMode)
}
