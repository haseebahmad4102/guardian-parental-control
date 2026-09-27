package com.example.guardian.core.model

/**
 * A single captured screen frame. Firestore path: `devices/{deviceId}/meta/screen`.
 *
 * [jpeg] is a base64-encoded, downscaled JPEG so it fits well under the 1 MB Firestore document
 * limit (keeps screen viewing on Firestore — no Firebase Storage needed). The parent decodes and
 * displays it; taps are mapped back as fractions of [width]×[height].
 */
data class ScreenFrame(
    val jpeg: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val capturedAt: Long = 0L,
)
