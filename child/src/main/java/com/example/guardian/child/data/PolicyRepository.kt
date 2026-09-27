package com.example.guardian.child.data

import com.example.guardian.child.data.local.PolicyDao
import com.example.guardian.child.data.local.PolicyEntity
import com.example.guardian.child.data.local.PolicyJson
import com.example.guardian.core.data.FirestorePaths
import com.example.guardian.core.model.Policy
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the child's policy. The Room cache is the source of truth for enforcement so limits hold
 * offline; Firestore updates flow into that cache. Read paths never hit the network.
 */
@Singleton
class PolicyRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val policyDao: PolicyDao,
    private val identityStore: DeviceIdentityStore,
) {
    /** Locally cached policy, reactive. Emits an empty [Policy] when nothing is cached yet. */
    fun localPolicy(): Flow<Policy> =
        policyDao.observe().map { it?.json?.let(PolicyJson::decode) ?: Policy() }

    suspend fun currentPolicy(): Policy =
        policyDao.get()?.json?.let(PolicyJson::decode) ?: Policy()

    suspend fun cache(policy: Policy) {
        policyDao.upsert(
            PolicyEntity(
                json = PolicyJson.encode(policy),
                updatedAt = policy.updatedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
            )
        )
    }

    /** One-shot fetch from Firestore into the cache. Returns null when offline/unavailable. */
    suspend fun syncOnce(): Policy? {
        val deviceId = identityStore.current?.deviceId ?: return null
        return runCatching {
            firestore.document(FirestorePaths.currentPolicy(deviceId)).get().await()
                .toObject(Policy::class.java)
        }.getOrNull()?.also { cache(it) }
    }

    /**
     * Real-time Firestore policy updates. The caller collects this and each emission is cached, so
     * `localPolicy()` (and thus enforcement) stays current while the device is online.
     */
    fun remotePolicyFlow(): Flow<Policy?> = callbackFlow {
        val deviceId = identityStore.current?.deviceId
        if (deviceId == null) { trySend(null); close(); return@callbackFlow }
        val reg = firestore.document(FirestorePaths.currentPolicy(deviceId))
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObject(Policy::class.java))
            }
        awaitClose { reg.remove() }
    }
}
