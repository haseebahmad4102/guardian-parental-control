package com.example.guardian.core.policy

import com.example.guardian.core.model.Bedtime
import com.example.guardian.core.model.Policy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyEvaluatorTest {

    private fun blocked(d: BlockDecision) = (d as? BlockDecision.Blocked)?.reason

    @Test
    fun `allowed when nothing applies`() {
        val decision = PolicyEvaluator.evaluate(
            pkg = "com.chrome", policy = Policy(),
            appMinutesToday = 10, totalMinutesToday = 10, dayOfWeek = 3, minuteOfDay = 600,
        )
        assertEquals(BlockDecision.Allowed, decision)
    }

    @Test
    fun `explicit block wins over everything`() {
        val policy = Policy(blockedApps = listOf("com.tiktok"))
        val decision = PolicyEvaluator.evaluate(
            "com.tiktok", policy, appMinutesToday = 0, totalMinutesToday = 0,
            dayOfWeek = 3, minuteOfDay = 600,
        )
        assertEquals(BlockReason.EXPLICITLY_BLOCKED, blocked(decision))
    }

    @Test
    fun `per-app limit blocks at or over the limit only`() {
        val policy = Policy(limits = mapOf("com.tiktok" to 60L))
        assertEquals(
            BlockDecision.Allowed,
            PolicyEvaluator.evaluate("com.tiktok", policy, 59, 59, 3, 600),
        )
        assertEquals(
            BlockReason.APP_LIMIT_REACHED,
            blocked(PolicyEvaluator.evaluate("com.tiktok", policy, 60, 60, 3, 600)),
        )
    }

    @Test
    fun `daily budget blocks any app once total is reached`() {
        val policy = Policy(dailyBudgetMinutes = 120L)
        assertEquals(
            BlockReason.DAILY_BUDGET_REACHED,
            blocked(PolicyEvaluator.evaluate("com.any", policy, 5, 120, 3, 600)),
        )
    }

    @Test
    fun `budget of zero means unlimited`() {
        val policy = Policy(dailyBudgetMinutes = 0L)
        assertEquals(
            BlockDecision.Allowed,
            PolicyEvaluator.evaluate("com.any", policy, 5, 10_000, 3, 600),
        )
    }

    @Test
    fun `bedtime same-day window`() {
        val bedtime = Bedtime(enabled = true, startMinuteOfDay = 13 * 60, endMinuteOfDay = 14 * 60)
        assertTrue(PolicyEvaluator.isWithinBedtime(bedtime, 3, 13 * 60 + 30))
        assertFalse(PolicyEvaluator.isWithinBedtime(bedtime, 3, 14 * 60)) // end is exclusive
        assertFalse(PolicyEvaluator.isWithinBedtime(bedtime, 3, 12 * 60))
    }

    @Test
    fun `bedtime window wrapping past midnight`() {
        val bedtime = Bedtime(enabled = true, startMinuteOfDay = 22 * 60, endMinuteOfDay = 7 * 60)
        assertTrue(PolicyEvaluator.isWithinBedtime(bedtime, 3, 23 * 60))  // 23:00
        assertTrue(PolicyEvaluator.isWithinBedtime(bedtime, 3, 2 * 60))   // 02:00
        assertFalse(PolicyEvaluator.isWithinBedtime(bedtime, 3, 12 * 60)) // noon
    }

    @Test
    fun `bedtime restricted to specific days`() {
        val bedtime = Bedtime(
            enabled = true, startMinuteOfDay = 22 * 60, endMinuteOfDay = 23 * 60,
            daysOfWeek = listOf(6, 7), // weekend only
        )
        assertTrue(PolicyEvaluator.isWithinBedtime(bedtime, 6, 22 * 60 + 30))
        assertFalse(PolicyEvaluator.isWithinBedtime(bedtime, 3, 22 * 60 + 30))
    }

    @Test
    fun `disabled bedtime never blocks`() {
        val bedtime = Bedtime(enabled = false, startMinuteOfDay = 0, endMinuteOfDay = 1439)
        assertFalse(PolicyEvaluator.isWithinBedtime(bedtime, 3, 600))
    }
}
