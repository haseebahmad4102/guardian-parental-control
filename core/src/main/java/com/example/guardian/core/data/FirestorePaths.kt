package com.example.guardian.core.data

/**
 * Single source of truth for Firestore collection/document paths and field names.
 * Both apps reference these so the schema can never drift between parent and child.
 *
 * Schema:
 * ```
 * users/{parentUid}
 * devices/{deviceId}
 * devices/{deviceId}/usage/{date}
 * devices/{deviceId}/notifications/{autoId}
 * devices/{deviceId}/policies/current
 * devices/{deviceId}/commands/{autoId}
 * pairing/{code}                      // short-lived pairing handshake docs
 * ```
 */
object FirestorePaths {
    const val USERS = "users"
    const val DEVICES = "devices"
    const val PAIRING = "pairing"

    // Sub-collections under a device document.
    const val USAGE = "usage"
    const val NOTIFICATIONS = "notifications"
    const val POLICIES = "policies"
    const val COMMANDS = "commands"
    const val STATUS = "status"
    const val META = "meta"

    /** The single policy document id under `devices/{deviceId}/policies`. */
    const val POLICY_CURRENT = "current"

    /** The single live-status document id under `devices/{deviceId}/status`. */
    const val STATUS_CURRENT = "current"

    /** The installed-apps document id under `devices/{deviceId}/meta`. */
    const val META_INSTALLED_APPS = "installedApps"

    /** The uninstall-request document id under `devices/{deviceId}/meta`. */
    const val META_UNINSTALL = "uninstall"

    /** The latest screen-frame document id under `devices/{deviceId}/meta`. */
    const val META_SCREEN = "screen"

    fun device(deviceId: String) = "$DEVICES/$deviceId"
    fun usage(deviceId: String) = "${device(deviceId)}/$USAGE"
    fun usageForDate(deviceId: String, date: String) = "${usage(deviceId)}/$date"
    fun notifications(deviceId: String) = "${device(deviceId)}/$NOTIFICATIONS"
    fun policies(deviceId: String) = "${device(deviceId)}/$POLICIES"
    fun currentPolicy(deviceId: String) = "${policies(deviceId)}/$POLICY_CURRENT"
    fun commands(deviceId: String) = "${device(deviceId)}/$COMMANDS"
    fun status(deviceId: String) = "${device(deviceId)}/$STATUS"
    fun currentStatus(deviceId: String) = "${status(deviceId)}/$STATUS_CURRENT"
    fun meta(deviceId: String) = "${device(deviceId)}/$META"
    fun installedApps(deviceId: String) = "${meta(deviceId)}/$META_INSTALLED_APPS"
    fun uninstallRequest(deviceId: String) = "${meta(deviceId)}/$META_UNINSTALL"
    fun screenFrame(deviceId: String) = "${meta(deviceId)}/$META_SCREEN"
    fun pairing(code: String) = "$PAIRING/$code"
}
