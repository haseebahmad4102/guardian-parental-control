package com.example.guardian.child.enforcement

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.guardian.child.admin.DeviceAdminManager
import com.example.guardian.child.data.MonitoringRepository
import com.example.guardian.child.data.PolicyRepository
import com.example.guardian.child.monitoring.InstalledAppsCollector
import com.example.guardian.child.monitoring.UsageStatsCollector
import com.example.guardian.core.policy.PolicyEvaluator
import com.example.guardian.core.util.DateKeys
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Periodic (every 15 min) re-apply of time-based enforcement, so bedtime and the daily budget
 * hold even with no network. It refreshes the policy cache when online, then — for device-wide
 * conditions (bedtime active, or budget reached) — locks the screen via Device Admin.
 *
 * Per-app blocking stays reactive in [GuardianAccessibilityService]; this worker covers the
 * time-window conditions that must fire without the child opening an app.
 */
@HiltWorker
class PolicyReapplyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val policyRepository: PolicyRepository,
    private val usageStatsCollector: UsageStatsCollector,
    private val deviceAdminManager: DeviceAdminManager,
    private val installedAppsCollector: InstalledAppsCollector,
    private val monitoringRepository: MonitoringRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Refresh the installed-app list so newly installed/removed apps reach the parent.
        runCatching { monitoringRepository.uploadInstalledApps(installedAppsCollector.collect()) }

        // Prefer a fresh pull; fall back to the offline cache if unreachable.
        val policy = policyRepository.syncOnce() ?: policyRepository.currentPolicy()

        val now = LocalDateTime.now()
        val withinBedtime = PolicyEvaluator.isWithinBedtime(
            policy.bedtime, now.dayOfWeek.value, now.hour * 60 + now.minute,
        )
        val snapshot = usageStatsCollector.collect(
            startOfDayMillis = DateKeys.startOfToday(),
            nowMillis = System.currentTimeMillis(),
        )
        val totalMinutes = snapshot.minutesByPackage.values.sum()
        val budgetReached = policy.dailyBudgetMinutes in 1..totalMinutes

        if ((withinBedtime || budgetReached) && deviceAdminManager.isActive()) {
            runCatching { deviceAdminManager.lockNow() }
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "policy_reapply"

        /** Schedules the 15-minute periodic re-apply (idempotent — keeps an existing schedule). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PolicyReapplyWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
