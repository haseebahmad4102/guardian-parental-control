package com.example.guardian.child.data

import com.example.guardian.core.data.FirestorePaths
import com.example.guardian.core.model.UninstallRequest
import com.example.guardian.core.model.UninstallStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Child side of the uninstall-approval flow: raise a request and observe the parent's decision. */
@Singleton
class UninstallRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val identityStore: DeviceIdentityStore,
) {
    /** Records that the child attempted to uninstall, moving the request to PENDING. */
    suspend fun requestUninstall() {
        val deviceId = identityStore.current?.deviceId ?: return
        firestore.document(FirestorePaths.uninstallRequest(deviceId))
            .set(
                mapOf(
                    "status" to UninstallStatus.PENDING.name,
                    "requestedAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            )
            .await()
    }

    /** Resets the request (e.g. after the allowed window elapses without an uninstall). */
    suspend fun reset() {
        val deviceId = identityStore.current?.deviceId ?: return
        firestore.document(FirestorePaths.uninstallRequest(deviceId))
            .set(mapOf("status" to UninstallStatus.NONE.name), SetOptions.merge())
            .await()
    }

    fun observe(): Flow<UninstallStatus> = callbackFlow {
        val deviceId = identityStore.current?.deviceId
        if (deviceId == null) { trySend(UninstallStatus.NONE); close(); return@callbackFlow }
        val reg = firestore.document(FirestorePaths.uninstallRequest(deviceId))
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val status = snap?.toObject(UninstallRequest::class.java)?.uninstallStatus
                    ?: UninstallStatus.NONE
                trySend(status)
            }
        awaitClose { reg.remove() }
    }
}
