package com.example.guardian.child.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.guardian.child.ui.theme.DevelopedByFooter

/**
 * First-launch consent + transparency screen. Lists exactly what will be monitored, in plain
 * language, and requires an explicit acknowledgement before anything is enabled.
 */
@Composable
fun ConsentScreen(onConsent: () -> Unit) {
    var acknowledged by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("This device is monitored", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "A parent or guardian has set up Guardian Family on this device. " +
                "While it is active, the following information is shared with them:",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))

        ConsentBullet("App usage — which apps are opened and for how long each day.")
        ConsentBullet(
            "Notification previews — the title and text of notifications as they appear. " +
                "Guardian cannot read the private contents of your chats or other apps."
        )
        ConsentBullet("Screen-time limits and app blocks the parent chooses to set.")
        ConsentBullet("The screen can be locked remotely, and a bedtime schedule may apply.")
        ConsentBullet("Approximate location, only if the parent enables that feature.")

        Spacer(Modifier.height(16.dp))
        Text(
            "Guardian shows a permanent notification while it runs — it does not hide itself. " +
                "You can view exactly what is active at any time from this app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = acknowledged,
                    role = Role.Checkbox,
                    onValueChange = { acknowledged = it },
                ),
        ) {
            // onCheckedChange = null: the whole Row owns the toggle semantics.
            Checkbox(checked = acknowledged, onCheckedChange = null)
            Spacer(Modifier.width(8.dp))
            Text(
                "I understand this device is monitored.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onConsent,
            enabled = acknowledged,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }
        Spacer(Modifier.height(16.dp))
        DevelopedByFooter()
    }
}

@Composable
private fun ConsentBullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("•  ", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
