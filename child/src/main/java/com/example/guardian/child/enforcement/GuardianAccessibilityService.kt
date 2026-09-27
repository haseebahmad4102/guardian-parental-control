package com.example.guardian.child.enforcement

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.util.Base64
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.guardian.child.R
import com.example.guardian.child.admin.DeviceAdminManager
import com.example.guardian.child.admin.DeviceOwnerManager
import com.example.guardian.child.data.PolicyRepository
import com.example.guardian.child.data.ScreenRepository
import com.example.guardian.child.data.UninstallRepository
import com.example.guardian.child.monitoring.UsageStatsCollector
import java.io.ByteArrayOutputStream
import com.example.guardian.core.model.Policy
import com.example.guardian.core.model.UninstallStatus
import com.example.guardian.core.policy.BlockDecision
import com.example.guardian.core.policy.PolicyEvaluator
import com.example.guardian.core.util.DateKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * The standard-permission app blocker + uninstall guard. Watches foreground-app changes and, when
 * a disallowed app comes forward, launches the full-screen [BlockActivity]. It also detects when
 * the child opens the system uninstall / device-admin-deactivation screen for Guardian and — unless
 * the parent has approved — cancels it and files an uninstall request for the parent to decide.
 *
 * Policy comes from the offline Room cache so blocking keeps working with no network.
 */
@AndroidEntryPoint
class GuardianAccessibilityService : AccessibilityService() {

    @Inject lateinit var policyRepository: PolicyRepository
    @Inject lateinit var usageStatsCollector: UsageStatsCollector
    @Inject lateinit var lockController: LockController
    @Inject lateinit var uninstallRepository: UninstallRepository
    @Inject lateinit var deviceAdminManager: DeviceAdminManager
    @Inject lateinit var deviceOwnerManager: DeviceOwnerManager
    @Inject lateinit var screenRepository: ScreenRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var policy: Policy = Policy()
    private var launcherPackage: String? = null
    private lateinit var appLabel: String

    /** True once the parent has approved uninstalling; the guard then lets the uninstall proceed. */
    @Volatile private var uninstallAllowed = false

    @Volatile private var lastBlockedPackage: String? = null
    @Volatile private var lastBlockAt: Long = 0L

    /** The app currently in the foreground, re-checked periodically to catch mid-session limits. */
    @Volatile private var currentForegroundPackage: String? = null

    /** Last takeScreenshot failure code (or -1), so the parent sees a precise reason. */
    @Volatile private var lastShotError: Int = -1

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        appLabel = getString(R.string.app_name)
        launcherPackage = resolveLauncherPackage()
        // Re-evaluate the current app periodically so a limit reached WHILE it's open still blocks
        // (window-state events only fire on app switches, not continuously).
        scope.launch {
            while (isActive) {
                delay(RECHECK_INTERVAL_MILLIS)
                val pkg = currentForegroundPackage
                if (pkg != null && !lockController.isLocked && !isExempt(pkg)) {
                    runCatching { evaluateAndEnforce(pkg) }
                }
            }
        }
        scope.launch { policyRepository.syncOnce() }
        scope.launch { policyRepository.remotePolicyFlow().collect { it?.let { p -> policyRepository.cache(p) } } }
        scope.launch { policyRepository.localPolicy().collect { policy = it } }
        // React to the parent's uninstall decision.
        scope.launch {
            uninstallRepository.observe().collect { status ->
                if (status == UninstallStatus.ALLOWED) {
                    uninstallAllowed = true
                    // Drop protection so the child can now remove the app.
                    deviceAdminManager.removeAdmin()
                    deviceOwnerManager.allowUninstall()
                } else {
                    uninstallAllowed = false
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        currentForegroundPackage = pkg

        // While the device is under a Guardian lock, keep the lock screen in front of everything.
        if (lockController.isLocked) {
            if (pkg != packageName) {
                runCatching { startActivity(LockScreenActivity.intent(this)) }
            }
            return
        }

        // Intercept attempts to uninstall / deactivate Guardian (unless the parent approved).
        if (handleUninstallAttempt(pkg)) return

        if (isExempt(pkg)) return
        scope.launch { evaluateAndEnforce(pkg) }
    }

    /**
     * Returns true if the current screen is a Guardian uninstall/deactivation attempt that we
     * handled. When not yet approved, we cancel it (Home) and file a request for the parent.
     */
    private fun handleUninstallAttempt(pkg: String): Boolean {
        if (pkg !in UNINSTALL_UI_PACKAGES && pkg != "com.android.settings") return false
        if (uninstallAllowed) return false // parent approved — let it through

        val text = windowText()
        val mentionsUs = text.contains(appLabel, ignoreCase = true)
        val isUninstallFlow = text.contains("uninstall", ignoreCase = true) ||
            text.contains("deactivate", ignoreCase = true) ||
            text.contains("device admin", ignoreCase = true)
        if (!mentionsUs || !isUninstallFlow) return false

        performGlobalAction(GLOBAL_ACTION_HOME)
        scope.launch { runCatching { uninstallRepository.requestUninstall() } }
        return true
    }

    /** Concatenates visible text of the active window so we can recognize the uninstall dialog. */
    private fun windowText(): String {
        val root = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        fun walk(node: AccessibilityNodeInfo?) {
            if (node == null) return
            node.text?.let { sb.append(it).append(' ') }
            node.contentDescription?.let { sb.append(it).append(' ') }
            for (i in 0 until node.childCount) walk(node.getChild(i))
        }
        runCatching { walk(root) }
        return sb.toString()
    }

    private suspend fun evaluateAndEnforce(pkg: String) {
        val currentPolicy = policy
        if (currentPolicy.blockedApps.isEmpty() &&
            currentPolicy.limits.isEmpty() &&
            currentPolicy.dailyBudgetMinutes == 0L &&
            !currentPolicy.bedtime.enabled
        ) return // nothing to enforce — avoid the usage query entirely

        val snapshot = usageStatsCollector.collect(
            startOfDayMillis = DateKeys.startOfToday(),
            nowMillis = System.currentTimeMillis(),
        )
        val now = LocalDateTime.now()
        val decision = PolicyEvaluator.evaluate(
            pkg = pkg,
            policy = currentPolicy,
            appMinutesToday = snapshot.minutesByPackage[pkg] ?: 0L,
            totalMinutesToday = snapshot.minutesByPackage.values.sum(),
            dayOfWeek = now.dayOfWeek.value,
            minuteOfDay = now.hour * 60 + now.minute,
        )

        if (decision is BlockDecision.Blocked && !lockController.isInGrace(pkg) && shouldLaunchBlock(pkg)) {
            val label = snapshot.labels[pkg] ?: pkg
            startActivity(BlockActivity.intent(this, pkg, label, decision.reason))
        }
    }

    private fun shouldLaunchBlock(pkg: String): Boolean {
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPackage && now - lastBlockAt < DEBOUNCE_MILLIS) return false
        lastBlockedPackage = pkg
        lastBlockAt = now
        return true
    }

    private fun isExempt(pkg: String): Boolean =
        pkg == packageName || pkg in SYSTEM_EXEMPT || pkg == launcherPackage

    private fun resolveLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
    }

    // ---- Remote screen view + control (driven by SCREENSHOT / TAP commands) ----

    /**
     * Captures the screen (Android 11+) and uploads a downscaled JPEG. Returns null on success, or
     * a human-readable error the parent can see (so a blank screen view is never silent).
     */
    suspend fun captureAndUpload(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "Screen view needs Android 11 or newer on the child device."
        }
        // Android rate-limits takeScreenshot; retry once after a short pause if the first fails.
        lastShotError = -1
        var software = withTimeoutOrNull(5_000L) { awaitScreenshot() }
        if (software == null) {
            delay(1_200L)
            software = withTimeoutOrNull(5_000L) { awaitScreenshot() }
        }
        if (software == null) return describeShotError(lastShotError)
        return runCatching { processAndUpload(software); null }
            .getOrElse { "Couldn't upload the screen: ${it.message}" }
    }

    private fun describeShotError(code: Int): String = when (code) {
        1 -> "Device blocked the capture (code 1). Some phones/secure screens don't allow accessibility screenshots."
        2 -> "Accessibility access denied (code 2). Re-enable Guardian under Accessibility."
        3 -> "Too many requests (code 3). Turn Live off and tap Refresh once."
        4 -> "Invalid display (code 4)."
        else -> "Screenshot didn't return in time (no callback). Your device may not support accessibility screenshots."
    }

    private suspend fun awaitScreenshot(): Bitmap? = suspendCancellableCoroutine { cont ->
        runCatching {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    // Copy to a software bitmap synchronously, BEFORE closing the buffer — the
                    // wrapped hardware bitmap becomes invalid once the buffer is closed.
                    val software = runCatching {
                        val buffer = result.hardwareBuffer
                        val hw = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                        val sw = hw?.copy(Bitmap.Config.ARGB_8888, false)
                        hw?.recycle()
                        buffer.close()
                        sw
                    }.getOrNull()
                    if (cont.isActive) cont.resume(software)
                }

                override fun onFailure(errorCode: Int) {
                    lastShotError = errorCode
                    if (cont.isActive) cont.resume(null)
                }
            })
        }.onFailure { if (cont.isActive) cont.resume(null) }
    }

    private suspend fun processAndUpload(software: Bitmap) {
        val scaled = if (software.width > TARGET_WIDTH) {
            val h = (software.height.toFloat() * TARGET_WIDTH / software.width).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(software, TARGET_WIDTH, h, true)
        } else software
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        runCatching { screenRepository.uploadFrame(base64, scaled.width, scaled.height) }
    }

    /** Injects a tap at a fractional screen position sent by the parent. */
    fun performTap(fx: Float, fy: Float) {
        val metrics = resources.displayMetrics
        val x = fx.coerceIn(0f, 1f) * metrics.widthPixels
        val y = fy.coerceIn(0f, 1f) * metrics.heightPixels
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    companion object {
        /** Set while the service is connected, so commands can trigger capture/tap. */
        @Volatile
        var instance: GuardianAccessibilityService? = null
            private set

        private const val TARGET_WIDTH = 600
        private const val JPEG_QUALITY = 35
        private const val DEBOUNCE_MILLIS = 2_000L
        private const val RECHECK_INTERVAL_MILLIS = 20_000L
        // Never block the system UI or Settings outright (avoids locking the user out of the device).
        val SYSTEM_EXEMPT = setOf("com.android.systemui", "com.android.settings")
        // Package-installer UIs that host the uninstall confirmation across OEMs.
        val UNINSTALL_UI_PACKAGES = setOf(
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.samsung.android.packageinstaller",
        )
    }
}
