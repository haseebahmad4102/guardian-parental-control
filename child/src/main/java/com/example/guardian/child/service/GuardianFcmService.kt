package com.example.guardian.child.service

import com.example.guardian.child.data.MonitoringRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FCM entry point. Its job in the command channel is delivery-of-last-resort: when the app is
 * backgrounded/killed and the live Firestore command listener isn't running, a data push wakes
 * the device and starts the monitor service, which then drains any pending commands.
 *
 * Real-time delivery while the app is alive comes from the Firestore listener, not FCM — so the
 * system works even without a server. A Cloud Function that sends the push on command creation is
 * documented in firebase/functions and is what makes background delivery reliable.
 */
@AndroidEntryPoint
class GuardianFcmService : FirebaseMessagingService() {

    @Inject lateinit var monitoringRepository: MonitoringRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { runCatching { monitoringRepository.updateFcmToken(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Wake the monitor service; it owns the command-draining listener.
        GuardianMonitorService.start(this)
    }

    override fun onDestroy() {
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }
}
