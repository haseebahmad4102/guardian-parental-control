package com.example.guardian.child.data

import com.example.guardian.core.data.FirestorePaths
import com.example.guardian.core.model.PairingSession
import com.example.guardian.core.model.PairingStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ClaimResult {
    data class Success(val identity: DeviceIdentity) : ClaimResult
    data class Error(val message: String) : ClaimResult
}

/**
 * Child side of pairing. Signs the device in anonymously, validates the parent's pairing code,
 * stamps this device's uid onto the target device document, and persists the resulting identity.
 */
@Singleton
class ChildPairingRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val identityStore: DeviceIdentityStore,
) {
    suspend fun claim(code: String): ClaimResult = runCatching {
        val childUid = ensureAnonymousUid()

        val pairingRef = firestore.document(FirestorePaths.pairing(code))
        val session = pairingRef.get().await().toObject(PairingSession::class.java)
            ?: return ClaimResult.Error("That code isn't valid. Check it and try again.")

        when {
            session.pairingStatus == PairingStatus.CLAIMED ->
                return ClaimResult.Error("That code has already been used.")
            session.pairingStatus != PairingStatus.PENDING ->
                return ClaimResult.Error("That code is no longer active.")
            session.isExpired(System.currentTimeMillis()) ->
                return ClaimResult.Error("That code has expired. Ask for a new one.")
            session.deviceId.isBlank() ->
                return ClaimResult.Error("That code is malformed. Ask for a new one.")
        }

        val now = System.currentTimeMillis()
        val deviceRef = firestore.document(FirestorePaths.device(session.deviceId))
        firestore.runBatch { batch ->
            batch.update(
                deviceRef,
                mapOf(
                    "childUid" to childUid,
                    "paired" to true,
                    "pairedAt" to now,
                ),
            )
            batch.update(
                pairingRef,
                mapOf(
                    "status" to PairingStatus.CLAIMED.name,
                    "childUid" to childUid,
                ),
            )
        }.await()

        val identity = DeviceIdentity(
            deviceId = session.deviceId,
            parentUid = session.parentUid,
            childName = session.childName,
        )
        identityStore.save(identity)
        ClaimResult.Success(identity)
    }.getOrElse {
        ClaimResult.Error(it.message ?: "Pairing failed. Check your connection and try again.")
    }

    private suspend fun ensureAnonymousUid(): String {
        auth.currentUser?.uid?.let { return it }
        return auth.signInAnonymously().await().user?.uid
            ?: error("Could not establish a device identity.")
    }
}
