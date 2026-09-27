package com.example.guardian.child.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guardian.child.ui.pair.ChildPairScreen

@Composable
fun ChildRoot() {
    val viewModel: ChildViewModel = hiltViewModel()
    val consented by viewModel.consented.collectAsStateWithLifecycle()
    val identity by viewModel.identity.collectAsStateWithLifecycle()

    when {
        !consented -> ConsentScreen(onConsent = viewModel::grantConsent)
        identity == null -> ChildPairScreen()
        else -> MonitoringScreen(identity!!)
    }
}
