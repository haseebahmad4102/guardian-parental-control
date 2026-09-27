package com.example.guardian.parent.data

import com.example.guardian.core.data.FirestorePaths
import com.example.guardian.core.model.Policy
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Parent side of policy: read/write the single `policies/current` doc the child enforces. */
@Singleton
class PolicyRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    fun observePolicy(deviceId: String): Flow<Policy> = callbackFlow {
        val reg = firestore.document(FirestorePaths.currentPolicy(deviceId))
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObject(Policy::class.java) ?: Policy())
            }
        awaitClose { reg.remove() }
    }

    /** Writes the full policy. The child's live listener + Room cache pick it up immediately. */
    suspend fun setPolicy(deviceId: String, policy: Policy) {
        firestore.document(FirestorePaths.currentPolicy(deviceId))
            .set(policy.copy(updatedAt = System.currentTimeMillis()))
            .await()
    }
}
