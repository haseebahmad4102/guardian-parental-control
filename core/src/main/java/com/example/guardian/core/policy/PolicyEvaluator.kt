package com.example.guardian.core.policy

import com.example.guardian.core.model.Bedtime
import com.example.guardian.core.model.Policy

/** Why an app is being blocked — surfaced to the child on the block screen and to the parent. */
enum class BlockReason {
    EXPLICITLY_BLOCKED,
    BEDTIME,
    DAILY_BUDGET_REACHED,
    APP_LIMIT_REACHED,
}

sealed interface BlockDecision {
    data object Allowed : BlockDecision
    data class Blocked(val reason: BlockReason) : BlockDecision
}

/**
 * Pure policy evaluation — the heart of enforcement, kept framework-free so it is exhaustively
 * unit-testable. The child's enforcer feeds it live inputs (which app, its minutes today, total
 * minutes today, and the current local time); the parent could use it for previews.
 *
 * Order of precedence: explicit block → bedtime → daily budget → per-app limit.
 */
object PolicyEvaluator {

    /**
     * @param pkg the package being opened
     * @param appMinutesToday foreground minutes for [pkg] so far today
     * @param totalMinutesToday foreground minutes across all apps today
     * @param dayOfWeek ISO day, 1 (Mon) .. 7 (Sun)
     * @param minuteOfDay local minutes since midnight, 0 .. 1439
     */
    fun evaluate(
        pkg: String,
        policy: Policy,
        appMinutesToday: Long,
        totalMinutesToday: Long,
        dayOfWeek: Int,
        minuteOfDay: Int,
    ): BlockDecision {
        if (pkg in policy.blockedApps) return BlockDecision.Blocked(BlockReason.EXPLICITLY_BLOCKED)

        if (isWithinBedtime(policy.bedtime, dayOfWeek, minuteOfDay)) {
            return BlockDecision.Blocked(BlockReason.BEDTIME)
        }

        if (policy.dailyBudgetMinutes in 1..totalMinutesToday) {
            return BlockDecision.Blocked(BlockReason.DAILY_BUDGET_REACHED)
        }

        val limit = policy.limits[pkg]
        if (limit != null && limit in 1..appMinutesToday) {
            return BlockDecision.Blocked(BlockReason.APP_LIMIT_REACHED)
        }

        return BlockDecision.Allowed
    }

    /**
     * True when [minuteOfDay] falls inside the bedtime window on [dayOfWeek]. The window may wrap
     * past midnight (start > end). An empty day set means "every day"; a non-empty set gates on the
     * window's start day.
     */
    fun isWithinBedtime(bedtime: Bedtime, dayOfWeek: Int, minuteOfDay: Int): Boolean {
        if (!bedtime.enabled) return false
        if (bedtime.daysOfWeek.isNotEmpty() && dayOfWeek !in bedtime.daysOfWeek) return false

        val start = bedtime.startMinuteOfDay
        val end = bedtime.endMinuteOfDay
        if (start == end) return false // zero-length window
        return if (start < end) {
            minuteOfDay in start until end
        } else {
            // Wraps midnight, e.g. 22:00 (1320) .. 07:00 (420).
            minuteOfDay >= start || minuteOfDay < end
        }
    }
}
