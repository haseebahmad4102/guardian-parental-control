package com.example.guardian.child.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.guardian.child.admin.DeviceOwnerManager
import com.example.guardian.child.command.CommandExecutor
import com.example.guardian.child.command.CommandResult
import com.example.guardian.child.data.CommandRepository
import com.example.guardian.child.data.DeviceIdentityStore
import com.example.guardian.child.data.MonitoringRepository
import com.example.guardian.child.data.PolicyRepository
import com.example.guardian.child.enforcement.LockController
import com.example.guardian.child.enforcement.LockScreenActivity
import com.example.guardian.child.enforcement.PolicyReapplyWorker
import com.example.guardian.child.monitoring.InstalledAppsCollector
import com.example.guardian.child.monitoring.MonitoringPermissions
import com.example.guardian.child.monitoring.UsageStatsCollector
import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.example.guardian.core.model.Command
import com.example.guardian.core.model.CommandStatus
import com.example.guardian.core.model.DeviceStatus
import com.example.guardian.core.model.OwnerMode
import com.example.guardian.core.util.DateKeys
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Collections
import javax.inject.Inject

private const val POLL_INTERVAL_MILLIS = 60_000L

/**
 * Persistent foreground service that polls usage every 60s and uploads it. The notification is
 * intentionally visible and honest — the OS requires it, and Guardian does not hide itself.
 *
 * The NotificationListenerService runs independently (the system binds it while access is
 * granted); this service owns the periodic usage poll and the live status heartbeat.
 */
@AndroidEntryPoint
class GuardianMonitorService : Service() {

    @Inject lateinit var collector: UsageStatsCollector
    @Inject lateinit var repository: MonitoringRepository
    @Inject lateinit var identityStore: DeviceIdentityStore
    @Inject lateinit var commandRepository: CommandRepository
    @Inject lateinit var commandExecutor: CommandExecutor
    @Inject lateinit var policyRepository: PolicyRepository
    @Inject lateinit var deviceOwnerManager: DeviceOwnerManager
    @Inject lateinit var lockController: LockController
    @Inject lateinit var installedAppsCollector: InstalledAppsCollector

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null
    private var commandJob: Job? = null
    private var policyJob: Job? = null
    @Volatile private var installedAppsUploaded = false

    /** Command ids already picked up this session, so a re-emit can't double-execute. */
    private val handledCommands = Collections.synchronizedSet(mutableSetOf<String>())

    /** Re-shows the Guardian lock screen when the device wakes while still locked. */
    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (lockController.isLocked) {
                runCatching { context.startActivity(LockScreenActivity.intent(context)) }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundNotification()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenOnReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (pollJob?.isActive != true) {
            pollJob = scope.launch { pollLoop() }
        }
        if (commandJob?.isActive != true) {
            commandJob = scope.launch { commandLoop() }
        }
        if (policyJob?.isActive != true) {
            // Keep the offline policy cache fresh, and (Device Owner) silently hide blocked apps.
            policyJob = scope.launch {
                policyRepository.remotePolicyFlow().collect { policy ->
                    policy?.let {
                        policyRepository.cache(it)
                        deviceOwnerManager.reconcileHiddenApps(it.blockedApps)
                    }
                }
            }
        }
        // If a parent lock is still in force (e.g. after reboot), re-assert the lock screen.
        if (lockController.isLocked) {
            runCatching { startActivity(LockScreenActivity.intent(this)) }
        }
        // Report the installed-app list once per process so the parent can limit any app.
        if (!installedAppsUploaded) {
            installedAppsUploaded = true
            scope.launch {
                if (identityStore.isPaired) {
                    runCatching { repository.uploadInstalledApps(installedAppsCollector.collect()) }
                }
            }
        }
        // Device Owner: ensure uninstall/factory-reset protection is in force.
        deviceOwnerManager.applyBaselineProtections()
        registerFcmToken()
        PolicyReapplyWorker.schedule(this)
        return START_STICKY
    }

    /** Live command channel: execute each pending command once, reporting the outcome back. */
    private suspend fun commandLoop() {
        commandRepository.observePending().collect { commands ->
            commands.forEach { command ->
                if (handledCommands.add(command.id)) {
                    scope.launch { runCatching { handleCommand(command) } }
                }
            }
        }
    }

    private suspend fun handleCommand(command: Command) {
        // Mark DELIVERED first so it drops out of the PENDING listener before we act.
        commandRepository.markStatus(command.id, CommandStatus.DELIVERED)
        when (val result = commandExecutor.execute(command)) {
            is CommandResult.Success ->
                commandRepository.markStatus(
                    command.id,
                    CommandStatus.EXECUTED,
                    executedAt = System.currentTimeMillis(),
                )
            is CommandResult.Failure ->
                commandRepository.markStatus(command.id, CommandStatus.FAILED, error = result.message)
        }
    }

    private fun registerFcmToken() {
        scope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                repository.updateFcmToken(token)
            }
        }
    }

    private suspend fun pollLoop() {
        while (scope.isActive) {
            runCatching { pollOnce() }
            delay(POLL_INTERVAL_MILLIS)
        }
    }

    private suspend fun pollOnce() {
        if (!identityStore.isPaired) return

        val hasUsage = MonitoringPermissions.hasUsageAccess(this)
        val hasNotif = MonitoringPermissions.hasNotificationAccess(this)
        val isDeviceOwner = deviceOwnerManager.isDeviceOwner()
        val ownerModeName = if (isDeviceOwner) OwnerMode.DEVICE_OWNER.name else OwnerMode.STANDARD.name
        val uninstallBlocked = deviceOwnerManager.isUninstallBlocked()

        if (hasUsage) {
            val snapshot = collector.collect(
                startOfDayMillis = DateKeys.startOfToday(),
                nowMillis = System.currentTimeMillis(),
            )
            repository.uploadUsage(snapshot.minutesByPackage, snapshot.labels)
            repository.updateStatus(
                DeviceStatus(
                    monitoringActive = true,
                    foregroundApp = snapshot.foregroundApp.orEmpty(),
                    foregroundAppLabel = snapshot.foregroundAppLabel.orEmpty(),
                    usageAccessGranted = true,
                    notificationAccessGranted = hasNotif,
                    ownerMode = ownerModeName,
                    uninstallBlocked = uninstallBlocked,
                )
            )
        } else {
            // Report that we're running but usage access is missing, so the parent sees the gap.
            repository.updateStatus(
                DeviceStatus(
                    monitoringActive = true,
                    usageAccessGranted = false,
                    notificationAccessGranted = hasNotif,
                    ownerMode = ownerModeName,
                    uninstallBlocked = uninstallBlocked,
                )
            )
        }
    }

    override fun onDestroy() {
        pollJob?.cancel()
        commandJob?.cancel()
        policyJob?.cancel()
        runCatching { unregisterReceiver(screenOnReceiver) }
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Guardian Family is active")
            .setContentText("This device is set up for family monitoring.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 0)
        }
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monitoring status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows that family monitoring is active. This notification cannot be hidden."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "guardian_monitor"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, GuardianMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GuardianMonitorService::class.java))
        }
    }
}
