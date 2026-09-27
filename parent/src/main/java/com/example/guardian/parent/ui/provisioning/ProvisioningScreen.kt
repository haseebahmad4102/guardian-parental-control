package com.example.guardian.parent.ui.provisioning

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.guardian.core.provisioning.ProvisioningPayload
import com.example.guardian.parent.util.QrGenerator

/** Flattened admin component of the child app — the DPC that becomes Device Owner. */
private const val CHILD_ADMIN_COMPONENT =
    "com.example.guardian.child/com.example.guardian.child.admin.GuardianDeviceAdminReceiver"

/**
 * Generates a **Device Owner provisioning QR**. The parent hosts the child APK, supplies its URL
 * and signing-cert checksum here, and the QR is scanned on a **factory-reset** device (tap the
 * setup-wizard welcome screen 6 times). See docs/DEVICE_OWNER_PROVISIONING.md.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvisioningScreen(onBack: () -> Unit) {
    var apkUrl by remember { mutableStateOf("") }
    var checksum by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var qr by remember { mutableStateOf<ImageBitmap?>(null) }

    val canGenerate = apkUrl.isNotBlank() && checksum.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Owner setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(24.dp),
        ) {
            Text(
                "Full control requires provisioning the child app as Device Owner on a " +
                    "factory-reset device. Host the child APK, paste its URL and signing checksum, " +
                    "then generate the QR. On the reset device's welcome screen, tap 6 times to open " +
                    "the QR scanner.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = apkUrl,
                onValueChange = { apkUrl = it; qr = null },
                label = { Text("Child APK URL (HTTPS)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = checksum,
                onValueChange = { checksum = it; qr = null },
                label = { Text("Signing-cert checksum (base64url)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = wifiSsid,
                onValueChange = { wifiSsid = it; qr = null },
                label = { Text("Wi-Fi SSID (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = wifiPassword,
                onValueChange = { wifiPassword = it; qr = null },
                label = { Text("Wi-Fi password (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val json = ProvisioningPayload.build(
                        componentName = CHILD_ADMIN_COMPONENT,
                        apkDownloadUrl = apkUrl.trim(),
                        signatureChecksum = checksum.trim(),
                        wifiSsid = wifiSsid.trim().ifBlank { null },
                        wifiPassword = wifiPassword.trim().ifBlank { null },
                    )
                    qr = QrGenerator.encode(json, sizePx = 640)
                },
                enabled = canGenerate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Generate provisioning QR")
            }

            qr?.let { bitmap ->
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Device Owner provisioning QR",
                        modifier = Modifier.size(280.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan on the factory-reset device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
