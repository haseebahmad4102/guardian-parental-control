package com.example.guardian.core.model

/**
 * The single active policy for a device. Firestore path: `devices/{deviceId}/policies/current`.
 *
 * The child caches this locally (Room, later phase) and WorkManager re-applies it every 15 min
 * so enforcement survives offline periods.
 */
data class Policy(
    /** Package names the child is not allowed to open at all. */
    val blockedApps: List<String> = emptyList(),
    /** Per-app daily limit in minutes: package -> minutes. */
    val limits: Map<String, Long> = emptyMap(),
    /** Total daily screen-time budget in minutes across all apps. 0 = no budget. */
    val dailyBudgetMinutes: Long = 0L,
    val bedtime: Bedtime = Bedtime(),
    /**
     * Special PIN the child can enter to unlock the device lock screen or grant a grace period on a
     * blocked/limit-exceeded app. Empty = no PIN set (PIN unlock disabled; only the parent can unlock).
     */
    val unlockPin: String = "",
    /** Minutes of access granted on a limit-exceeded app after the correct PIN is entered. */
    val pinGraceMinutes: Long = 15L,
    val updatedAt: Long = 0L,
)

/**
 * A recurring nightly lock window, expressed in minutes-from-midnight local time.
 * A window may wrap past midnight (e.g. start 22:00 = 1320, end 07:00 = 420).
 */
data class Bedtime(
    val enabled: Boolean = false,
    val startMinuteOfDay: Int = 0,
    val endMinuteOfDay: Int = 0,
    /** Days the window applies to, as java.time.DayOfWeek values 1 (Mon) .. 7 (Sun). Empty = every day. */
    val daysOfWeek: List<Int> = emptyList(),
)
