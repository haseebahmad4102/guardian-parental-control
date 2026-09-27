package com.example.guardian.child.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device-Owner-only capabilities (the "full power" path). Every method is a safe no-op unless the
 * child app is actually the Device Owner, so callers can invoke them unconditionally.
 *
 * Unlocks over the standard path: silent app hiding (a blocked app simply disappears instead of
 * being overlaid), uninstall protection, and factory-reset protection.
 */
@Singleton
class DeviceOwnerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    /**
     * Applies the always-on protections for a managed device: block uninstalling this app, and
     * disallow factory reset / adding users / safe boot (which could bypass monitoring).
     */
    fun applyBaselineProtections() {
        if (!isDeviceOwner()) return
        runCatching {
            dpm.setUninstallBlocked(admin, context.packageName, true)
            dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
            dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER)
            dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
            // Allow this app to pin itself (kiosk). While pinned with LOCK_TASK_FEATURE_NONE, the
            // home/recents keys, status bar, and the long-press power "global actions" menu are all
            // suppressed — so the child can't leave the lock screen or reach the software power menu.
            dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
            dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
        }
    }

    fun isUninstallBlocked(): Boolean =
        isDeviceOwner() && runCatching { dpm.isUninstallBlocked(admin, context.packageName) }
            .getOrDefault(false)

    /**
     * Drops uninstall protection so the app can be removed (after a parent-approved request). On a
     * Device Owner the owner role must be cleared too, or the uninstall is still refused.
     */
    fun allowUninstall() {
        if (!isDeviceOwner()) return
        runCatching {
            dpm.setUninstallBlocked(admin, context.packageName, false)
            @Suppress("DEPRECATION")
            dpm.clearDeviceOwnerApp(context.packageName)
        }
    }

    /**
     * Silently hides exactly the [blockedPackages] and un-hides anything we previously hid that is
     * no longer blocked. Never touches this app or packages Guardian didn't hide.
     */
    fun reconcileHiddenApps(blockedPackages: List<String>) {
        if (!isDeviceOwner()) return
        val target = blockedPackages.filter { it != context.packageName }.toSet()
        val previouslyHidden = readHidden()

        val nowHidden = LinkedHashSet<String>()
        for (pkg in target) {
            val ok = runCatching { dpm.setApplicationHidden(admin, pkg, true) }.getOrDefault(false)
            if (ok) nowHidden.add(pkg)
        }
        // Un-hide apps we hid before that are no longer blocked.
        for (pkg in previouslyHidden - target) {
            runCatching { dpm.setApplicationHidden(admin, pkg, false) }
        }
        writeHidden(nowHidden)
    }

    /** Relinquishes Device Owner (used for un-enrollment). */
    fun releaseOwnership() {
        if (!isDeviceOwner()) return
        runCatching {
            reconcileHiddenApps(emptyList())
            @Suppress("DEPRECATION")
            dpm.clearDeviceOwnerApp(context.packageName)
        }
    }

    private fun readHidden(): Set<String> =
        prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()

    private fun writeHidden(set: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN, set).apply()
    }

    private companion object {
        const val PREFS = "guardian_device_owner"
        const val KEY_HIDDEN = "hidden_packages"
    }
}
