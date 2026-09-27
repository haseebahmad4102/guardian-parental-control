package com.example.guardian.child.data

import com.example.guardian.core.data.FirestorePaths
import com.example.guardian.core.model.Command
import com.example.guardian.core.model.CommandStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Child side of the command channel: observe pending commands and report their outcome. */
@Singleton
class CommandRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val identityStore: DeviceIdentityStore,
) {
    /** Streams commands still awaiting execution. Single equality filter — no index required. */
    fun observePending(): Flow<List<Command>> = callbackFlow {
        val deviceId = identityStore.current?.deviceId
        if (deviceId == null) {
            trySend(emptyList()); close(); return@callbackFlow
        }
        val reg = firestore.collection(FirestorePaths.commands(deviceId))
            .whereEqualTo("status", CommandStatus.PENDING.name)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val commands = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Command::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(commands)
            }
        awaitClose { reg.remove() }
    }

    suspend fun markStatus(
        commandId: String,
        status: CommandStatus,
        error: String? = null,
        executedAt: Long? = null,
    ) {
        val deviceId = identityStore.current?.deviceId ?: return
        val update = buildMap<String, Any> {
            put("status", status.name)
            error?.let { put("error", it) }
            executedAt?.let { put("executedAt", it) }
        }
        firestore.collection(FirestorePaths.commands(deviceId))
            .document(commandId)
            .update(update)
            .await()
    }
}
