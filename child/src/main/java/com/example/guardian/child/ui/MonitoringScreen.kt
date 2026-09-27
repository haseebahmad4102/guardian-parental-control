package com.example.guardian.child.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.example.guardian.child.data.DeviceIdentity
import com.example.guardian.child.monitoring.MonitoringPermissions
import com.example.guardian.child.service.GuardianMonitorService
import com.example.guardian.child.ui.theme.DevelopedByFooter

private data class PermState(
    val usage: Boolean,
    val notifications: Boolean,
    val postNotifications: Boolean,
    val remoteLock: Boolean,
    val appBlocking: Boolean,
)

/**
 * Paired-state screen: transparency about what's active plus a permission checklist that
 * degrades gracefully — each missing grant disables one data stream and offers a Settings
 * shortcut, but never blocks the app. Starts the monitor service once permissions allow.
 */
@Composable
fun MonitoringScreen(identity: DeviceIdentity) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun read() = PermState(
        usage = MonitoringPermissions.hasUsageAccess(context),
        notifications = MonitoringPermissions.hasNotificationAccess(context),
        postNotifications = MonitoringPermissions.hasPostNotifications(context),
        remoteLock = MonitoringPermissions.hasRemoteLock(context),
        appBlocking = MonitoringPermissions.hasAppBlocking(context),
    )

    var perms by remember { mutableStateOf(read()) }

    // Re-check permissions whenever we return from a system Settings screen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) perms = read()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val postNotifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { perms = read() }

    // Start (or keep alive) the monitor service once the device is paired.
    LaunchedEffect(Unit) { GuardianMonitorService.start(context) }

    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(scheme.primaryContainer, scheme.secondaryContainer.copy(alpha = 0.3f), scheme.surface)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Guardian Family",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            buildString {
                append("This device is linked to a parent account")
                if (identity.childName.isNotBlank()) append(" as \"${identity.childName}\"")
                append(". Monitoring is visible and cannot be hidden.")
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))

        val fullyActive = perms.usage && perms.notifications
        StatusBanner(fullyActive)
        Spacer(Modifier.height(16.dp))

        // Once everything is granted, let the child dismiss the app to the background; the
        // foreground service keeps monitoring and enforcing.
        val allGranted = perms.usage && perms.notifications && perms.appBlocking &&
            perms.remoteLock && perms.postNotifications
        if (allGranted) {
            Button(
                onClick = {
                    GuardianMonitorService.start(context)
                    (context as? Activity)?.moveTaskToBack(true)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done — run in background")
            }
            Spacer(Modifier.height(16.dp))
        }

        if (MonitoringPermissions.isDeviceOwner(context)) {
            DeviceOwnerCard()
            Spacer(Modifier.height(16.dp))
        }

        PermissionCard(
            title = "Usage access",
            granted = perms.usage,
            rationale = "Lets Guardian report which apps are used and for how long. Without it, " +
                "screen-time data is unavailable.",
            onGrant = { context.startActivity(MonitoringPermissions.usageAccessSettings()) },
        )
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            title = "Notification access",
            granted = perms.notifications,
            rationale = "Lets Guardian forward notification previews (title and text only) to the " +
                "parent. Message contents inside other apps are never accessible.",
            onGrant = { context.startActivity(MonitoringPermissions.notificationListenerSettings()) },
        )
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            title = "Screen access & control (Accessibility)",
            granted = perms.appBlocking,
            rationale = "Powers app blocking, time limits, uninstall protection, and letting the parent " +
                "view & tap the screen. Tap Grant → find \"Guardian Family\" → turn it On → Allow.",
            onGrant = { context.startActivity(MonitoringPermissions.appBlockingSettings()) },
        )
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            title = "Remote lock",
            granted = perms.remoteLock,
            rationale = "Lets the parent lock this screen remotely. Without it, lock commands can't " +
                "run.",
            onGrant = {
                context.startActivity(
                    MonitoringPermissions.remoteLockSettings(
                        context,
                        "Guardian needs this to lock the screen when your parent asks.",
                    )
                )
            },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Spacer(Modifier.height(12.dp))
            PermissionCard(
                title = "Show notifications",
                granted = perms.postNotifications,
                rationale = "Required so the ongoing \"monitoring is active\" notification is visible.",
                onGrant = { postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            )
        }

        Spacer(Modifier.height(24.dp))
        DevelopedByFooter()
    }
}

@Composable
private fun DeviceOwnerCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Device Owner — full protection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "This device is fully managed. Blocked apps are hidden, and Guardian can't be " +
                    "uninstalled or the device factory-reset without your parent.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StatusBanner(active: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (active) "Monitoring active" else "Monitoring partially active",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (active) "Usage and notifications are being shared with the parent."
                else "Some permissions are missing below. Granting them enables full monitoring.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    rationale: String,
    onGrant: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (granted) "Granted" else "Not granted",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (granted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(rationale, style = MaterialTheme.typography.bodySmall)
            if (!granted) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onGrant) { Text("Grant") }
            }
        }
    }
}
