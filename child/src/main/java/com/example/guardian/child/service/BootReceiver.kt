package com.example.guardian.child.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.guardian.child.consent.ConsentManager
import com.example.guardian.child.data.DeviceIdentityStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Restarts the monitor service after a reboot, but only if the device is paired and consented. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var identityStore: DeviceIdentityStore
    @Inject lateinit var consentManager: ConsentManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (identityStore.isPaired && consentManager.consented.value) {
            GuardianMonitorService.start(context)
        }
    }
}
