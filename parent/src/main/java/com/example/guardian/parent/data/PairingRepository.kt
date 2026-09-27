package com.example.guardian.parent.data

import com.example.guardian.core.data.FirestorePaths
import com.example.guardian.core.model.Device
import com.example.guardian.core.model.OwnerMode
import com.example.guardian.core.model.PairingSession
import com.example.guardian.core.model.PairingStatus
import com.example.guardian.core.pairing.PairingCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** How long a freshly generated pairing code stays claimable. */
private const val PAIRING_TTL_MILLIS = 10 * 60 * 1000L

@Singleton
class PairingRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    /**
     * Creates the target device document plus a pending pairing document, atomically, and
     * returns the session the parent should display. Throws if not signed in or on write failure.
     */
    suspend fun startSession(childName: String): PairingSession {
        val parentUid = auth.currentUser?.uid
            ?: error("Must be signed in to pair a device.")

        val deviceId = UUID.randomUUID().toString()
        val code = reserveUniqueCode()
        val now = System.currentTimeMillis()

        val device = Device(
            deviceId = deviceId,
            parentUid = parentUid,
            childName = childName.trim(),
            paired = false,
            pairedAt = 0L,
            ownerMode = OwnerMode.STANDARD.name,
        )
        val session = PairingSession(
            code = code,
            parentUid = parentUid,
            deviceId = deviceId,
            childName = childName.trim(),
            status = PairingStatus.PENDING.name,
            createdAt = now,
            expiresAt = now + PAIRING_TTL_MILLIS,
        )

        firestore.runBatch { batch ->
            batch.set(firestore.document(FirestorePaths.device(deviceId)), device)
            batch.set(firestore.document(FirestorePaths.pairing(code)), session)
        }.await()

        return session
    }

    /** Streams the pairing document so the UI can react when the child claims it. */
    fun observeSession(code: String): Flow<PairingSession?> = callbackFlow {
        val registration = firestore.document(FirestorePaths.pairing(code))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(PairingSession::class.java))
            }
        awaitClose { registration.remove() }
    }

    /** Cancels an outstanding session: removes the pairing doc and the device doc if unclaimed. */
    suspend fun cancelSession(session: PairingSession) {
        firestore.document(FirestorePaths.pairing(session.code)).delete().await()
        val deviceRef = firestore.document(FirestorePaths.device(session.deviceId))
        val device = deviceRef.get().await().toObject(Device::class.java)
        if (device != null && !device.paired) {
            deviceRef.delete().await()
        }
    }

    /** Finds a code not currently in use. Collisions are astronomically rare; retry a few times. */
    private suspend fun reserveUniqueCode(attempts: Int = 5): String {
        repeat(attempts) {
            val code = PairingCode.generate()
            val exists = firestore.document(FirestorePaths.pairing(code)).get().await().exists()
            if (!exists) return code
        }
        error("Could not allocate a pairing code. Please try again.")
    }
}
