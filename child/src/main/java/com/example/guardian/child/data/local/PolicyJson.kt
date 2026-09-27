package com.example.guardian.child.data.local

import com.example.guardian.core.model.Bedtime
import com.example.guardian.core.model.Policy
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializes [Policy] to/from JSON for the Room cache using Android's built-in `org.json`
 * (no extra dependency). Decoding is defensive — a malformed cache degrades to an empty policy
 * rather than throwing.
 */
object PolicyJson {

    fun encode(policy: Policy): String = JSONObject().apply {
        put("blockedApps", JSONArray(policy.blockedApps))
        put("limits", JSONObject(policy.limits.mapValues { it.value }))
        put("dailyBudgetMinutes", policy.dailyBudgetMinutes)
        put("bedtime", JSONObject().apply {
            put("enabled", policy.bedtime.enabled)
            put("startMinuteOfDay", policy.bedtime.startMinuteOfDay)
            put("endMinuteOfDay", policy.bedtime.endMinuteOfDay)
            put("daysOfWeek", JSONArray(policy.bedtime.daysOfWeek))
        })
        put("unlockPin", policy.unlockPin)
        put("pinGraceMinutes", policy.pinGraceMinutes)
        put("updatedAt", policy.updatedAt)
    }.toString()

    fun decode(json: String): Policy = runCatching {
        val root = JSONObject(json)
        Policy(
            blockedApps = root.optJSONArray("blockedApps").toStringList(),
            limits = root.optJSONObject("limits").toLongMap(),
            dailyBudgetMinutes = root.optLong("dailyBudgetMinutes", 0L),
            bedtime = root.optJSONObject("bedtime")?.let { b ->
                Bedtime(
                    enabled = b.optBoolean("enabled", false),
                    startMinuteOfDay = b.optInt("startMinuteOfDay", 0),
                    endMinuteOfDay = b.optInt("endMinuteOfDay", 0),
                    daysOfWeek = b.optJSONArray("daysOfWeek").toIntList(),
                )
            } ?: Bedtime(),
            unlockPin = root.optString("unlockPin", ""),
            pinGraceMinutes = root.optLong("pinGraceMinutes", 15L),
            updatedAt = root.optLong("updatedAt", 0L),
        )
    }.getOrDefault(Policy())

    private fun JSONArray?.toStringList(): List<String> =
        this?.let { (0 until it.length()).map { i -> it.getString(i) } } ?: emptyList()

    private fun JSONArray?.toIntList(): List<Int> =
        this?.let { (0 until it.length()).map { i -> it.getInt(i) } } ?: emptyList()

    private fun JSONObject?.toLongMap(): Map<String, Long> {
        if (this == null) return emptyMap()
        val result = LinkedHashMap<String, Long>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = getLong(key)
        }
        return result
    }
}
