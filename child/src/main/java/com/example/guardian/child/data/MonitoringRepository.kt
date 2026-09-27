package com.example.guardian.child.data

import com.example.guardian.core.data.FirestorePaths
import com.example.guardian.core.model.DeviceStatus
import com.example.guardian.core.model.InstalledApp
import com.example.guardian.core.model.InstalledAppsDoc
import com.example.guardian.core.model.NotificationEvent
import com.example.guardian.core.model.UsageRecord
import com.example.guardian.core.util.DateKeys
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads monitoring data under the paired device's sub-collections. All methods no-op when the
 * device isn't paired yet (no identity), so callers don't need to guard every call.
 */
@Singleton
class MonitoringRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val identityStore: DeviceIdentityStore,
) {
    /** Writes today's per-app usage snapshot to `usage/{today}` (full overwrite of the day doc). */
    suspend fun uploadUsage(minutes: Map<String, Long>, labels: Map<String, String>) {
        val deviceId = identityStore.current?.deviceId ?: return
        val today = DateKeys.today()
        val record = UsageRecord(
            date = today,
            apps = minutes,
            labels = labels,
            updatedAt = System.currentTimeMillis(),
        )
        firestore.document(FirestorePaths.usageForDate(deviceId, today)).set(record).await()
    }

    /** Uploads the child's launchable-app list so the parent can limit/block any installed app. */
    suspend fun uploadInstalledApps(apps: List<InstalledApp>) {
        val deviceId = identityStore.current?.deviceId ?: return
        firestore.document(FirestorePaths.installedApps(deviceId))
            .set(InstalledAppsDoc(apps = apps, updatedAt = System.currentTimeMillis()))
            .await()
    }

    /** Appends a single observed notification to `notifications/{autoId}`. */
    suspend fun addNotification(event: NotificationEvent) {
        val deviceId = identityStore.current?.deviceId ?: return
        firestore.collection(FirestorePaths.notifications(deviceId)).add(event).await()
    }

    /** Merges the live status doc so the parent can see monitoring health in real time. */
    suspend fun updateStatus(status: DeviceStatus) {
        val deviceId = identityStore.current?.deviceId ?: return
        firestore.document(FirestorePaths.currentStatus(deviceId))
            .set(status.copy(updatedAt = System.currentTimeMillis()), SetOptions.merge())
            .await()
    }

    /**
     * Persists the FCM token so a server/Cloud Function can wake this device for commands. Written
     * as a field-level merge into the same status doc — deliberately NOT part of [DeviceStatus], so
     * the periodic status writes above never overwrite it.
     */
    suspend fun updateFcmToken(token: String) {
        val deviceId = identityStore.current?.deviceId ?: return
        firestore.document(FirestorePaths.currentStatus(deviceId))
            .set(
                mapOf("fcmToken" to token, "fcmUpdatedAt" to System.currentTimeMillis()),
                SetOptions.merge(),
            )
            .await()
    }
}
