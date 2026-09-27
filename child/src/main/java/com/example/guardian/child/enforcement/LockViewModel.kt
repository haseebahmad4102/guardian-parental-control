package com.example.guardian.child.enforcement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardian.child.data.PolicyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val lockController: LockController,
    policyRepository: PolicyRepository,
) : ViewModel() {

    val locked: StateFlow<Boolean> = lockController.locked

    private val unlockPin: StateFlow<String> = policyRepository.localPolicy()
        .map { it.unlockPin }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val pinAvailable: Boolean get() = unlockPin.value.isNotEmpty()

    /** Returns true if [pin] matches the parent's unlock PIN (and unlocks). */
    fun submitPin(pin: String): Boolean {
        val expected = unlockPin.value
        return if (expected.isNotEmpty() && pin == expected) {
            lockController.unlock()
            true
        } else false
    }
}
