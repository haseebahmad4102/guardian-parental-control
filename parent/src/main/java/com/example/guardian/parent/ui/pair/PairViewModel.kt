package com.example.guardian.parent.ui.pair

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardian.core.model.PairingSession
import com.example.guardian.core.model.PairingStatus
import com.example.guardian.core.pairing.PairingPayload
import com.example.guardian.parent.data.PairingRepository
import com.example.guardian.parent.util.QrGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PairUiState {
    /** Collecting the child's name before generating a code. */
    data object EnterName : PairUiState
    data object Generating : PairUiState

    /** Code + QR shown; waiting for a child device to claim it. */
    data class Waiting(
        val session: PairingSession,
        val qr: ImageBitmap,
    ) : PairUiState

    data class Paired(val childName: String) : PairUiState
    data class Error(val message: String) : PairUiState
}

@HiltViewModel
class PairViewModel @Inject constructor(
    private val repo: PairingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PairUiState>(PairUiState.EnterName)
    val state: StateFlow<PairUiState> = _state.asStateFlow()

    private var observeJob: Job? = null
    private var activeSession: PairingSession? = null

    fun startPairing(childName: String) {
        if (_state.value is PairUiState.Generating) return
        _state.value = PairUiState.Generating
        viewModelScope.launch {
            runCatching { repo.startSession(childName) }
                .onSuccess { session ->
                    activeSession = session
                    val qr = QrGenerator.encode(PairingPayload.encode(session.code))
                    _state.value = PairUiState.Waiting(session, qr)
                    observe(session)
                }
                .onFailure { _state.value = PairUiState.Error(it.message ?: "Could not start pairing.") }
        }
    }

    private fun observe(session: PairingSession) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repo.observeSession(session.code).collect { updated ->
                when (updated?.pairingStatus) {
                    PairingStatus.CLAIMED -> {
                        observeJob?.cancel()
                        _state.value = PairUiState.Paired(updated.childName.ifBlank { session.childName })
                    }
                    else -> Unit // still PENDING (or transient null) — keep waiting.
                }
            }
        }
    }

    /** Cancels an outstanding session; safe to call when nothing is active. */
    fun cancel() {
        observeJob?.cancel()
        val session = activeSession ?: return
        activeSession = null
        viewModelScope.launch { runCatching { repo.cancelSession(session) } }
        _state.value = PairUiState.EnterName
    }

    override fun onCleared() {
        observeJob?.cancel()
        super.onCleared()
    }
}
