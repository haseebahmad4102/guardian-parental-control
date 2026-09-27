package com.example.guardian.core.model

/**
 * A short-lived pairing handshake. Firestore path: `pairing/{code}`, where `code` is the
 * 6-digit code the parent shows and the child enters/scans.
 *
 * The parent creates this together with the target `devices/{deviceId}` document. The child
 * reads it to learn which device to claim, then flips [status] to CLAIMED. [expiresAt] is an
 * epoch-millis deadline enforced both client-side and in the security rules.
 */
data class PairingSession(
    val code: String = "",
    val parentUid: String = "",
    val deviceId: String = "",
    val childName: String = "",
    val status: String = PairingStatus.PENDING.name,
    val childUid: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
) {
    val pairingStatus: PairingStatus get() = PairingStatus.fromValue(status)

    fun isExpired(nowMillis: Long): Boolean = expiresAt in 1..nowMillis
}
