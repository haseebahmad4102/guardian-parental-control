package com.example.guardian.parent.data

import com.example.guardian.core.data.FirestorePaths
import com.example.guardian.core.model.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevicesRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    /**
     * Streams every device owned by the current parent. Emits an empty list when signed out.
     * Includes not-yet-claimed devices so a pairing in progress is visible; the UI can filter
     * on [Device.paired].
     */
    fun observeDevices(): Flow<List<Device>> = callbackFlow {
        val parentUid = auth.currentUser?.uid
        if (parentUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = firestore.collection(FirestorePaths.DEVICES)
            .whereEqualTo("parentUid", parentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // Sorted client-side (newest first) to avoid needing a composite index.
                val devices = snapshot?.toObjects(Device::class.java).orEmpty()
                    .sortedByDescending { it.pairedAt }
                trySend(devices)
            }
        awaitClose { registration.remove() }
    }
}
