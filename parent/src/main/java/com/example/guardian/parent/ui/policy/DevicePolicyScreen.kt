package com.example.guardian.parent.ui.policy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guardian.core.model.Bedtime
import com.example.guardian.core.model.Policy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePolicyScreen(
    onBack: () -> Unit,
    viewModel: DevicePolicyViewModel = hiltViewModel(),
) {
    val remote by viewModel.policy.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    // Editable working copy, re-seeded whenever a new remote version arrives.
    var working by remember(remote) { mutableStateOf(remote) }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(saved) {
        if (saved) { snackbar.showSnackbar("Saved"); viewModel.consumeSaved() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Limits & blocking") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(working) }, enabled = !saving) {
                        Text("Save")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            item {
                BudgetSection(
                    minutes = working.dailyBudgetMinutes,
                    onChange = { working = working.copy(dailyBudgetMinutes = it) },
                )
            }
            item { Spacer(Modifier.height(20.dp)); HorizontalDivider(); Spacer(Modifier.height(20.dp)) }
            item {
                UnlockPinSection(
                    pin = working.unlockPin,
                    graceMinutes = working.pinGraceMinutes,
                    onPinChange = { working = working.copy(unlockPin = it) },
                    onGraceChange = { working = working.copy(pinGraceMinutes = it) },
                )
            }
            item { Spacer(Modifier.height(20.dp)); HorizontalDivider(); Spacer(Modifier.height(20.dp)) }
            item {
                BedtimeSection(
                    bedtime = working.bedtime,
                    onChange = { working = working.copy(bedtime = it) },
                )
            }
            item { Spacer(Modifier.height(20.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp)) }
            item {
                Text("Apps", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                if (apps.isEmpty()) {
                    Text(
                        "Apps appear here once the child device reports usage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(apps, key = { it.pkg }) { app ->
                AppPolicyRow(
                    app = app,
                    blocked = app.pkg in working.blockedApps,
                    limitMinutes = working.limits[app.pkg],
                    onBlockedChange = { blocked ->
                        working = working.copy(
                            blockedApps = if (blocked) working.blockedApps + app.pkg
                            else working.blockedApps - app.pkg
                        )
                    },
                    onLimitChange = { minutes ->
                        working = working.copy(
                            limits = if (minutes != null && minutes > 0)
                                working.limits + (app.pkg to minutes)
                            else working.limits - app.pkg
                        )
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun BudgetSection(minutes: Long, onChange: (Long) -> Unit) {
    Text("Daily screen-time budget", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    MinutesField(
        value = minutes,
        placeholder = "No limit",
        onChange = onChange,
    )
    Text(
        "Total minutes allowed across all apps per day. Leave empty for no limit.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun UnlockPinSection(
    pin: String,
    graceMinutes: Long,
    onPinChange: (String) -> Unit,
    onGraceChange: (Long) -> Unit,
) {
    Text("Unlock PIN", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = pin,
        onValueChange = { onPinChange(it.filter(Char::isDigit).take(8)) },
        label = { Text("PIN (digits)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.width(180.dp),
    )
    Text(
        "The child can enter this PIN to unlock the device, or to get more time on a limited app. " +
            "Leave empty to allow unlocking only from here.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    Spacer(Modifier.height(12.dp))
    Text("Extra minutes granted by the PIN on a limited app", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(4.dp))
    MinutesField(
        value = graceMinutes,
        placeholder = "Minutes",
        onChange = { onGraceChange(if (it > 0) it else 15L) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BedtimeSection(bedtime: Bedtime, onChange: (Bedtime) -> Unit) {
    var picking by remember { mutableStateOf<PickTarget?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Bedtime lock", style = MaterialTheme.typography.titleMedium)
        Switch(
            checked = bedtime.enabled,
            onCheckedChange = { onChange(bedtime.copy(enabled = it)) },
        )
    }

    if (bedtime.enabled) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TimeButton("From", bedtime.startMinuteOfDay) { picking = PickTarget.START }
            TimeButton("To", bedtime.endMinuteOfDay) { picking = PickTarget.END }
        }
        Spacer(Modifier.height(12.dp))
        DayOfWeekChips(
            selected = bedtime.daysOfWeek.toSet(),
            onToggle = { day ->
                val next = if (day in bedtime.daysOfWeek) bedtime.daysOfWeek - day
                else bedtime.daysOfWeek + day
                onChange(bedtime.copy(daysOfWeek = next.sorted()))
            },
        )
        Text(
            "No days selected means every night.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    picking?.let { target ->
        val initial = if (target == PickTarget.START) bedtime.startMinuteOfDay else bedtime.endMinuteOfDay
        TimePickerDialog(
            initialMinuteOfDay = initial,
            onDismiss = { picking = null },
            onConfirm = { minuteOfDay ->
                onChange(
                    if (target == PickTarget.START) bedtime.copy(startMinuteOfDay = minuteOfDay)
                    else bedtime.copy(endMinuteOfDay = minuteOfDay)
                )
                picking = null
            },
        )
    }
}

private enum class PickTarget { START, END }

@Composable
private fun TimeButton(label: String, minuteOfDay: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("$label  ${formatTime(minuteOfDay)}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOfWeekChips(selected: Set<Int>, onToggle: (Int) -> Unit) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label ->
            val day = index + 1
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialMinuteOfDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
    )
}

@Composable
private fun AppPolicyRow(
    app: AppInfo,
    blocked: Boolean,
    limitMinutes: Long?,
    onBlockedChange: (Boolean) -> Unit,
    onLimitChange: (Long?) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(160.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Block", style = MaterialTheme.typography.labelMedium)
                Checkbox(checked = blocked, onCheckedChange = onBlockedChange)
            }
        }
        if (!blocked) {
            MinutesField(
                value = limitMinutes ?: 0L,
                placeholder = "Daily limit (min)",
                onChange = { onLimitChange(it) },
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun MinutesField(
    value: Long,
    placeholder: String,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = if (value > 0) value.toString() else "",
        onValueChange = { raw ->
            val digits = raw.filter(Char::isDigit).take(4)
            onChange(digits.toLongOrNull() ?: 0L)
        },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.width(180.dp),
    )
}

private fun formatTime(minuteOfDay: Int): String {
    val h = (minuteOfDay / 60) % 24
    val m = minuteOfDay % 60
    return "%02d:%02d".format(h, m)
}
