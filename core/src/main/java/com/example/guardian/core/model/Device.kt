package com.example.guardian.core.model

/**
 * A paired child device. Firestore path: `devices/{deviceId}`.
 *
 * All fields have defaults so Firestore's reflection-based deserialization can construct
 * the object with a no-arg constructor. [ownerMode] is stored as a raw String and read
 * through [mode] so an unknown value written by a newer client never crashes an older one.
 */
data class Device(
    val deviceId: String = "",
    val parentUid: String = "",
    /** The child device's (anonymous) auth uid, stamped in when it claims the pairing code. */
    val childUid: String = "",
    val childName: String = "",
    /** True once a child device has claimed this doc. False while a pairing code is outstanding. */
    val paired: Boolean = false,
    val pairedAt: Long = 0L,
    val ownerMode: String = OwnerMode.STANDARD.name,
) {
    val mode: OwnerMode get() = OwnerMode.fromValue(ownerMode)
}
