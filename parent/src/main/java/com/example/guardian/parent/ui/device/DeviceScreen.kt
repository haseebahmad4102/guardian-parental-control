package com.example.guardian.parent.ui.device

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guardian.core.model.Command
import com.example.guardian.core.model.CommandStatus
import com.example.guardian.core.model.DeviceStatus
import com.example.guardian.core.model.NotificationEvent
import com.example.guardian.core.model.OwnerMode
import com.example.guardian.core.model.UninstallStatus
import com.example.guardian.parent.util.formatMinutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    onBack: () -> Unit,
    onOpenPolicy: () -> Unit,
    onViewScreen: () -> Unit,
    viewModel: DeviceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val commands by viewModel.commands.collectAsStateWithLifecycle()
    val sendingLock by viewModel.sendingLock.collectAsStateWithLifecycle()
    val uninstallStatus by viewModel.uninstallStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.device?.childName?.ifBlank { "Device" } ?: "Device") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onViewScreen) {
                        Icon(Icons.AutoMirrored.Filled.ScreenShare, contentDescription = "View screen")
                    }
                    IconButton(onClick = onOpenPolicy) {
                        Icon(Icons.Filled.Tune, contentDescription = "Limits & blocking")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            item { MonitoringHeader(state.status) }

            if (uninstallStatus == UninstallStatus.PENDING) {
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    UninstallRequestCard(
                        onAllow = { viewModel.decideUninstall(true) },
                        onDeny = { viewModel.decideUninstall(false) },
                    )
                }
            }
            item { Spacer(Modifier.height(20.dp)) }

            item {
                ControlsCard(
                    sending = sendingLock,
                    lastCommand = commands.firstOrNull(),
                    onLock = viewModel::lockDevice,
                    onUnlock = viewModel::unlockDevice,
                )
            }
            item { Spacer(Modifier.height(20.dp)) }

            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Screen time", style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatMinutes(state.usage.totalMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    RangeToggle(selected = state.range, onSelect = viewModel::setRange)
                    Spacer(Modifier.height(16.dp))
                    if (state.usage.isEmpty) {
                        Text(
                            "No usage recorded for this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        UsageChart(apps = state.usage.perApp)
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
            item {
                Text(
                    "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            if (state.notifications.isEmpty()) {
                item {
                    Text(
                        "No notifications captured yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.notifications, key = { it.id }) { event ->
                    NotificationRow(event)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun UninstallRequestCard(onAllow: () -> Unit, onDeny: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Uninstall requested",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "This device tried to uninstall Guardian. It stays installed unless you allow it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDeny,
                    modifier = Modifier.weight(1f),
                ) { Text("Keep installed") }
                OutlinedButton(
                    onClick = onAllow,
                    modifier = Modifier.weight(1f),
                ) { Text("Allow uninstall") }
            }
        }
    }
}

@Composable
private fun ControlsCard(
    sending: Boolean,
    lastCommand: Command?,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
) {
    SectionCard {
        Text("Controls", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onLock, enabled = !sending, modifier = Modifier.weight(1f)) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Text("  Lock")
                }
            }
            OutlinedButton(onClick = onUnlock, enabled = !sending, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.LockOpen, contentDescription = null)
                Text("  Unlock")
            }
        }
        if (lastCommand != null) {
            Spacer(Modifier.height(10.dp))
            val when_ = DateUtils.getRelativeTimeSpanString(lastCommand.createdAt).toString()
            val failed = lastCommand.commandStatus == CommandStatus.FAILED
            Text(
                "Last: ${lastCommand.type} — ${lastCommand.status.lowercase()} ($when_)",
                style = MaterialTheme.typography.bodySmall,
                color = if (failed) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (failed && !lastCommand.error.isNullOrBlank()) {
                Text(
                    lastCommand.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MonitoringHeader(status: DeviceStatus?) {
    SectionCard {
        when {
            status == null -> {
                Text("Waiting for the device", style = MaterialTheme.typography.titleMedium)
                Text(
                    "No status reported yet. The child app reports in once monitoring starts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Text(
                    if (status.monitoringActive) "Monitoring active" else "Monitoring off",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (status.foregroundAppLabel.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Currently open: ${status.foregroundAppLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (status.mode == OwnerMode.DEVICE_OWNER)
                        "Device Owner — full control (apps hidden when blocked, uninstall protected)"
                    else "Standard permissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val gaps = buildList {
                    if (!status.usageAccessGranted) add("usage access")
                    if (!status.notificationAccessGranted) add("notification access")
                }
                if (gaps.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Not granted on device: ${gaps.joinToString(", ")}. That data is unavailable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeToggle(selected: UsageRange, onSelect: (UsageRange) -> Unit) {
    val options = UsageRange.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, range ->
            SegmentedButton(
                selected = selected == range,
                onClick = { onSelect(range) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(range.label)
            }
        }
    }
}

@Composable
private fun NotificationRow(event: NotificationEvent) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                event.appLabel.ifBlank { event.pkg },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                DateUtils.getRelativeTimeSpanString(event.ts).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (event.title.isNotBlank()) {
            Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        if (event.text.isNotBlank()) {
            Text(event.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
