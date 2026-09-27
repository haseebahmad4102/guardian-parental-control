package com.example.guardian.child.enforcement

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guardian.child.admin.DeviceOwnerManager
import com.example.guardian.child.ui.theme.DevelopedByFooter
import com.example.guardian.child.ui.theme.GuardianChildTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Full-screen Guardian lock. Under Device Owner it pins itself with lock-task (kiosk): no Home,
 * Recents, Back, status bar, or software power menu — and it re-asserts on every reboot. A forced
 * hardware power-off can't be prevented by any app, but the lock survives the reboot.
 */
@AndroidEntryPoint
class LockScreenActivity : ComponentActivity() {

    @Inject lateinit var deviceOwnerManager: DeviceOwnerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContent {
            GuardianChildTheme {
                LockContent(onUnlocked = { exitLock() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Enter kiosk lock-task so the child can't leave the lock screen (Device Owner only).
        if (deviceOwnerManager.isDeviceOwner()) {
            runCatching { startLockTask() }
        }
    }

    private fun exitLock() {
        runCatching { stopLockTask() }
        finish()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, LockScreenActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

@Composable
private fun LockContent(
    onUnlocked: () -> Unit,
    viewModel: LockViewModel = hiltViewModel(),
) {
    val locked by viewModel.locked.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(locked) { if (!locked) onUnlocked() }
    BackHandler(enabled = true) { /* swallow back */ }

    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(scheme.primary, scheme.primaryContainer, scheme.surface)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(scheme.surface.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(64.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Device Locked",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your parent has locked this device. It will unlock when they allow it, or with the " +
                    "correct PIN.",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onPrimary.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8); error = false },
                label = { Text("Unlock PIN") },
                singleLine = true,
                isError = error,
                supportingText = if (error) { { Text("Incorrect PIN") } } else null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (!viewModel.submitPin(pin)) { error = true; pin = "" } },
                enabled = pin.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.onPrimary,
                    contentColor = scheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Unlock")
            }
            Spacer(Modifier.height(24.dp))
            DevelopedByFooter()
        }
    }
}
