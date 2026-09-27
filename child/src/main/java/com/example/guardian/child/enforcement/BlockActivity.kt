package com.example.guardian.child.enforcement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.guardian.child.ui.theme.DevelopedByFooter
import com.example.guardian.child.ui.theme.GuardianChildTheme
import com.example.guardian.core.policy.BlockReason
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen block screen launched over a disallowed / limit-exceeded app. It can be cleared by
 * going Home, or — like the device lock — by entering the special PIN, which grants a short grace
 * window on that app (and returns the child to it). The parent can also just raise the limit.
 */
@AndroidEntryPoint
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PKG).orEmpty()
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty()
        val reason = BlockReason.entries.getOrNull(intent.getIntExtra(EXTRA_REASON, -1))

        setContent {
            GuardianChildTheme {
                BlockContent(
                    pkg = pkg,
                    appLabel = appLabel,
                    reason = reason,
                    onGoHome = ::goHome,
                    onGranted = ::finish, // return to the app; grace now active
                )
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }

    override fun onUserLeaveHint() {
        finish()
    }

    companion object {
        private const val EXTRA_PKG = "pkg"
        private const val EXTRA_APP_LABEL = "app_label"
        private const val EXTRA_REASON = "reason"

        fun intent(context: Context, pkg: String, appLabel: String, reason: BlockReason): Intent =
            Intent(context, BlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_PKG, pkg)
                putExtra(EXTRA_APP_LABEL, appLabel)
                putExtra(EXTRA_REASON, reason.ordinal)
            }
    }
}

@Composable
private fun BlockContent(
    pkg: String,
    appLabel: String,
    reason: BlockReason?,
    onGoHome: () -> Unit,
    onGranted: () -> Unit,
    viewModel: BlockViewModel = hiltViewModel(),
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Blocked", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = reasonMessage(appLabel.ifBlank { "This app" }, reason),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )

            if (viewModel.pinAvailable) {
                Spacer(Modifier.height(28.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8); error = false },
                    label = { Text("Enter PIN for more time") },
                    singleLine = true,
                    isError = error,
                    supportingText = if (error) { { Text("Incorrect PIN") } } else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (viewModel.submitPin(pkg, pin)) onGranted() else { error = true; pin = "" }
                    },
                    enabled = pin.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Unlock with PIN")
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onGoHome) { Text("Close") }
            Spacer(Modifier.height(24.dp))
            DevelopedByFooter()
        }
    }
}

private fun reasonMessage(app: String, reason: BlockReason?): String = when (reason) {
    BlockReason.EXPLICITLY_BLOCKED -> "$app has been blocked by your parent."
    BlockReason.BEDTIME -> "It's bedtime. $app is unavailable right now."
    BlockReason.DAILY_BUDGET_REACHED -> "You've reached today's total screen-time limit."
    BlockReason.APP_LIMIT_REACHED -> "You've reached today's time limit for $app."
    null -> "$app is unavailable right now."
}
