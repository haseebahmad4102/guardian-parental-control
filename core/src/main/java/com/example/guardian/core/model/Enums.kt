package com.example.guardian.core.model

/**
 * How the child device was provisioned. Determines which enforcement APIs are available.
 *
 * - [DEVICE_OWNER]: full control (app hiding, silent policy, uninstall protection, kiosk).
 * - [STANDARD]: permission-based fallback (Device Admin + Accessibility + Usage + Notification
 *   access), each user-granted. Less powerful, no factory reset required.
 */
enum class OwnerMode {
    DEVICE_OWNER,
    STANDARD,
    /** Value used when a document was written by an older/unknown client. */
    UNKNOWN;

    companion object {
        fun fromValue(value: String?): OwnerMode =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

/** Remote command types the parent can issue and the child executes. */
enum class CommandType {
    /** Immediately lock the screen (DevicePolicyManager.lockNow) and show the Guardian lock screen. */
    LOCK,
    /** Clear a parent-initiated lock (dismisses the Guardian lock screen). */
    UNLOCK,
    /** Add/remove an app from the blocked set. payload: { "package": ..., "blocked": "true|false" }. */
    BLOCK,
    /** Set a per-app daily time limit. payload: { "package": ..., "minutes": ... }. */
    SET_LIMIT,
    /** Set the daily total screen-time budget. payload: { "minutes": ... }. */
    SET_BUDGET,
    /** Push a refreshed policy so the child re-syncs from policies/current. */
    REFRESH_POLICY,
    /** Capture a screenshot and upload it for the parent to view. */
    SCREENSHOT,
    /** Launch an app on the child. payload: { "package": ... }. */
    OPEN_APP,
    /** Inject a tap at a fractional screen position. payload: { "fx": 0..1, "fy": 0..1 }. */
    TAP,
    UNKNOWN;

    companion object {
        fun fromValue(value: String?): CommandType =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

/** Lifecycle of a pairing handshake document. */
enum class PairingStatus {
    /** Created by the parent, waiting for a child device to claim it. */
    PENDING,
    /** A child device has claimed the code and stamped itself onto the device doc. */
    CLAIMED,
    /** The code's validity window elapsed before it was claimed. */
    EXPIRED,
    UNKNOWN;

    companion object {
        fun fromValue(value: String?): PairingStatus =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

/** Lifecycle of a queued command. */
enum class CommandStatus {
    PENDING,
    DELIVERED,
    EXECUTED,
    FAILED,
    UNKNOWN;

    companion object {
        fun fromValue(value: String?): CommandStatus =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}
