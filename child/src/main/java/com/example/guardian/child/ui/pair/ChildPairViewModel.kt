package com.example.guardian.child.ui.pair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardian.child.data.ChildPairingRepository
import com.example.guardian.child.data.ClaimResult
import com.example.guardian.core.pairing.PairingPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChildPairUiState(
    val submitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChildPairViewModel @Inject constructor(
    private val repo: ChildPairingRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ChildPairUiState())
    val ui: StateFlow<ChildPairUiState> = _ui.asStateFlow()

    fun clearError() { _ui.value = _ui.value.copy(error = null) }

    /** [raw] may be a typed 6-digit code or a scanned QR payload; both are normalized here. */
    fun submit(raw: String) {
        if (_ui.value.submitting) return
        val code = PairingPayload.decode(raw)
        if (code == null) {
            _ui.value = ChildPairUiState(error = "Enter the 6-digit code shown on the parent's phone.")
            return
        }
        _ui.value = ChildPairUiState(submitting = true)
        viewModelScope.launch {
            // On success the DeviceIdentityStore updates and ChildRoot swaps to the status screen.
            when (val result = repo.claim(code)) {
                is ClaimResult.Success -> _ui.value = ChildPairUiState(submitting = false)
                is ClaimResult.Error -> _ui.value = ChildPairUiState(submitting = false, error = result.message)
            }
        }
    }
}
