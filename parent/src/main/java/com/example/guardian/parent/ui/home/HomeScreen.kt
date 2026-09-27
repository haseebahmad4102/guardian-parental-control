package com.example.guardian.parent.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guardian.core.model.Device
import com.example.guardian.parent.ui.theme.DevelopedByFooter
import java.text.DateFormat
import java.util.Date

/**
 * Parent dashboard: lists paired child devices and offers pairing a new one. Tapping a device
 * will open its detail dashboard in Phase 4 ([onOpenDevice] is wired but currently inert).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPair: () -> Unit,
    onSignOut: () -> Unit,
    onOpenDevice: (Device) -> Unit,
    onSetupDeviceOwner: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guardian") },
                actions = {
                    IconButton(onClick = onSetupDeviceOwner) {
                        Icon(Icons.Filled.QrCode2, contentDescription = "Device Owner setup")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Filled.Logout, contentDescription = "Sign out")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Pair a device") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = onPair,
            )
        },
        bottomBar = { DevelopedByFooter() },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (devices.isEmpty()) {
                EmptyState(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(devices, key = { it.deviceId }) { device ->
                        DeviceRow(
                            device = device,
                            onClick = { if (device.paired) onOpenDevice(device) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: Device, onClick: () -> Unit) {
    val subtitle = if (device.paired) {
        "Paired " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(device.pairedAt))
    } else {
        "Pairing in progress…"
    }
    ListItem(
        headlineContent = { Text(device.childName.ifBlank { "Unnamed device" }) },
        supportingContent = { Text(subtitle) },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (device.paired) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No devices yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap \"Pair a device\" and enter the code on your child's phone.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
