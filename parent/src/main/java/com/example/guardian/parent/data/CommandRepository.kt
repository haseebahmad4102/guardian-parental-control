package com.example.guardian.parent.data

import com.example.guardian.core.data.FirestorePaths
import com.example.guardian.core.model.Command
import com.example.guardian.core.model.CommandStatus
import com.example.guardian.core.model.CommandType
import com.example.guardian.core.model.UninstallStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Parent side of the command channel: enqueue commands and watch their status. */
@Singleton
class CommandRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    suspend fun sendLock(deviceId: String) {
        enqueue(deviceId, CommandType.LOCK)
    }

    suspend fun sendUnlock(deviceId: String) {
        enqueue(deviceId, CommandType.UNLOCK)
    }

    suspend fun sendScreenshot(deviceId: String) {
        enqueue(deviceId, CommandType.SCREENSHOT)
    }

    suspend fun sendOpenApp(deviceId: String, pkg: String) {
        enqueue(deviceId, CommandType.OPEN_APP, mapOf("package" to pkg))
    }

    suspend fun sendTap(deviceId: String, fx: Float, fy: Float) {
        enqueue(deviceId, CommandType.TAP, mapOf("fx" to fx.toString(), "fy" to fy.toString()))
    }

    /** Approves or refuses a child's uninstall request. ALLOWED lets the child remove the app. */
    suspend fun decideUninstall(deviceId: String, allowed: Boolean) {
        val status = if (allowed) UninstallStatus.ALLOWED else UninstallStatus.DENIED
        firestore.document(FirestorePaths.uninstallRequest(deviceId))
            .set(
                mapOf("status" to status.name, "decidedAt" to System.currentTimeMillis()),
                SetOptions.merge(),
            )
            .await()
    }

    private suspend fun enqueue(
        deviceId: String,
        type: CommandType,
        payload: Map<String, String> = emptyMap(),
    ) {
        val command = Command(
            type = type.name,
            payload = payload,
            status = CommandStatus.PENDING.name,
            createdAt = System.currentTimeMillis(),
        )
        firestore.collection(FirestorePaths.commands(deviceId)).add(command).await()
    }

    /** Most recent commands (newest first) so the UI can show delivery/execution status. */
    fun observeCommands(deviceId: String, limit: Long = 10): Flow<List<Command>> = callbackFlow {
        val reg = firestore.collection(FirestorePaths.commands(deviceId))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val commands = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Command::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(commands)
            }
        awaitClose { reg.remove() }
    }
}
