package com.example.guardian.child.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.guardian.child.data.DeviceIdentityStore
import com.example.guardian.child.data.MonitoringRepository
import com.example.guardian.core.model.NotificationEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Forwards notification *previews* (title + text) to Firestore. The system binds this service
 * whenever the user has granted Notification Access; it runs independently of the foreground
 * monitor service.
 *
 * PLATFORM LIMITATION: only the posted preview is available here — never another app's private
 * message database. This is the real ceiling for "reading WhatsApp/TikTok messages".
 */
@AndroidEntryPoint
class GuardianNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var repository: MonitoringRepository
    @Inject lateinit var identityStore: DeviceIdentityStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val event = sbn?.let(::toEvent) ?: return
        if (!identityStore.isPaired) return
        scope.launch { runCatching { repository.addNotification(event) } }
    }

    private fun toEvent(sbn: StatusBarNotification): NotificationEvent? {
        if (sbn.packageName == packageName) return null
        // Skip the collapsed "group summary" placeholder; the child notifications carry the content.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null

        return NotificationEvent(
            pkg = sbn.packageName,
            appLabel = labelFor(sbn.packageName),
            title = title,
            text = text,
            ts = sbn.postTime,
        )
    }

    private fun labelFor(pkg: String): String = runCatching {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(pkg)

    override fun onDestroy() {
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }
}
