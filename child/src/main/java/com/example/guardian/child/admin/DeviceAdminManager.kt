package com.example.guardian.child.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Wraps Device Admin state + the force-lock action for the standard-permission path. */
@Singleton
class DeviceAdminManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val component = ComponentName(context, GuardianDeviceAdminReceiver::class.java)

    fun isActive(): Boolean = dpm.isAdminActive(component)

    /** Intent that opens the system "activate device admin?" screen. */
    fun enableIntent(explanation: String): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)
        }

    /** Locks the screen immediately. Throws SecurityException if admin isn't active. */
    fun lockNow() = dpm.lockNow()

    /**
     * Deactivates this app as a device admin so it can be uninstalled. Called only after the parent
     * approves an uninstall request (a device-admin app can't be removed while admin is active).
     */
    fun removeAdmin() {
        if (isActive()) runCatching { dpm.removeActiveAdmin(component) }
    }
}
