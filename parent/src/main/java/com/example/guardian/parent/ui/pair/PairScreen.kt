package com.example.guardian.parent.ui.pair

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Parent-side pairing. Collects the child's name, generates a code + QR, and waits for the
 * child device to claim it. [onDone] returns to the dashboard (used on success and on back).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(
    onDone: () -> Unit,
    viewModel: PairViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pair a device") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.cancel(); onDone() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val s = state) {
                is PairUiState.EnterName -> EnterNameStep(onGenerate = viewModel::startPairing)
                is PairUiState.Generating -> Centered { CircularProgressIndicator() }
                is PairUiState.Waiting -> WaitingStep(s)
                is PairUiState.Paired -> PairedStep(childName = s.childName, onDone = onDone)
                is PairUiState.Error -> ErrorStep(message = s.message, onRetry = { viewModel.cancel() })
            }
        }
    }
}

@Composable
private fun EnterNameStep(onGenerate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Text(
        "Give this child device a name, then generate a code to enter on it.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Child name (e.g. Amir's phone)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = { onGenerate(name) },
        enabled = name.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Generate pairing code")
    }
}

@Composable
private fun WaitingStep(state: PairUiState.Waiting) {
    Text("Enter this code on the child device", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(16.dp))
    Text(
        text = state.session.code.chunked(3).joinToString(" "),
        fontSize = 44.sp,
        letterSpacing = 8.sp,
        style = MaterialTheme.typography.displaySmall,
    )
    Spacer(Modifier.height(24.dp))
    Image(
        bitmap = state.qr,
        contentDescription = "Pairing QR code",
        modifier = Modifier.size(220.dp),
    )
    Spacer(Modifier.height(24.dp))
    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    Spacer(Modifier.height(8.dp))
    Text(
        "Waiting for the child device… the code expires in 10 minutes.",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PairedStep(childName: String, onDone: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Text("Paired!", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(
        "\"$childName\" is now linked to your account.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text("Done")
    }
}

@Composable
private fun ErrorStep(message: String, onRetry: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onRetry) { Text("Try again") }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
