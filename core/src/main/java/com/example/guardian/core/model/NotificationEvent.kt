package com.example.guardian.core.model

/**
 * A single notification observed on the child device. Firestore path:
 * `devices/{deviceId}/notifications/{autoId}`.
 *
 * PLATFORM LIMITATION: this captures only the notification *preview* (title + text the app
 * chose to post). It is NOT the message content stored in another app's private database.
 * Reading WhatsApp/TikTok/etc. internal data is not possible without root.
 */
data class NotificationEvent(
    val id: String = "",
    val pkg: String = "",
    val appLabel: String = "",
    val title: String = "",
    val text: String = "",
    val ts: Long = 0L,
)
