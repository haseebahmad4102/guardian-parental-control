package com.example.guardian.child.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.example.guardian.child.service.GuardianMonitorService

/**
 * Device Admin receiver. On the standard path, being an active admin with the `force-lock`
 * policy is what allows [android.app.admin.DevicePolicyManager.lockNow].
 *
 * On the Device Owner path, [onProfileProvisioningComplete] fires once provisioning finishes; we
 * apply the baseline protections (uninstall + factory-reset block) and start the monitor service.
 */
class GuardianDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        // Admin activated — remote lock is now available.
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        // Now Device Owner: lock down and start monitoring. (Auto-pairing from the admin-extras
        // bundle can be added here in a later refinement.)
        DeviceOwnerManager(context.applicationContext).applyBaselineProtections()
        GuardianMonitorService.start(context)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Turning off Guardian device admin disables remote lock. Your parent will be notified."
}
