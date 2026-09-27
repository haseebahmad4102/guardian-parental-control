package com.example.guardian.parent.util

/** Formats a minute count as a compact duration, e.g. 0 → "0m", 75 → "1h 15m", 120 → "2h". */
fun formatMinutes(minutes: Long): String {
    if (minutes <= 0) return "0m"
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours == 0L -> "${mins}m"
        mins == 0L -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}
