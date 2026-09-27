package com.example.guardian.parent.data

import com.example.guardian.core.data.FirestorePaths
import com.example.guardian.core.model.Device
import com.example.guardian.core.model.DeviceStatus
import com.example.guardian.core.model.InstalledAppsDoc
import com.example.guardian.core.model.NotificationEvent
import com.example.guardian.core.model.ScreenFrame
import com.example.guardian.core.model.UninstallRequest
import com.example.guardian.core.model.UninstallStatus
import com.example.guardian.core.model.UsageRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Read-only, real-time views of a single child device's data for the parent dashboard. */
@Singleton
class DeviceDataRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    fun observeDevice(deviceId: String): Flow<Device?> = callbackFlow {
        val reg = firestore.document(FirestorePaths.device(deviceId))
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObject(Device::class.java))
            }
        awaitClose { reg.remove() }
    }

    fun observeScreenFrame(deviceId: String): Flow<ScreenFrame?> = callbackFlow {
        val reg = firestore.document(FirestorePaths.screenFrame(deviceId))
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObject(ScreenFrame::class.java))
            }
        awaitClose { reg.remove() }
    }

    fun observeInstalledApps(deviceId: String): Flow<InstalledAppsDoc?> = callbackFlow {
        val reg = firestore.document(FirestorePaths.installedApps(deviceId))
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObject(InstalledAppsDoc::class.java))
            }
        awaitClose { reg.remove() }
    }

    fun observeUninstallStatus(deviceId: String): Flow<UninstallStatus> = callbackFlow {
        val reg = firestore.document(FirestorePaths.uninstallRequest(deviceId))
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(
                    snap?.toObject(UninstallRequest::class.java)?.uninstallStatus
                        ?: UninstallStatus.NONE
                )
            }
        awaitClose { reg.remove() }
    }

    fun observeStatus(deviceId: String): Flow<DeviceStatus?> = callbackFlow {
        val reg = firestore.document(FirestorePaths.currentStatus(deviceId))
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObject(DeviceStatus::class.java))
            }
        awaitClose { reg.remove() }
    }

    /** Most recent [days] daily usage docs (newest first). Single-field order — no index needed. */
    fun observeUsage(deviceId: String, days: Long = 14): Flow<List<UsageRecord>> = callbackFlow {
        val reg = firestore.collection(FirestorePaths.usage(deviceId))
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(days)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(UsageRecord::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun observeNotifications(deviceId: String, limit: Long = 100): Flow<List<NotificationEvent>> =
        callbackFlow {
            val reg = firestore.collection(FirestorePaths.notifications(deviceId))
                .orderBy("ts", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener { snap, err ->
                    if (err != null) { close(err); return@addSnapshotListener }
                    val events = snap?.documents?.mapNotNull { doc ->
                        doc.toObject(NotificationEvent::class.java)?.copy(id = doc.id)
                    }.orEmpty()
                    trySend(events)
                }
            awaitClose { reg.remove() }
        }
}
