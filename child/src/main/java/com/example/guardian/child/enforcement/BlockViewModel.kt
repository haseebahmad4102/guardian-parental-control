package com.example.guardian.child.enforcement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardian.child.data.PolicyRepository
import com.example.guardian.core.model.Policy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BlockViewModel @Inject constructor(
    private val lockController: LockController,
    policyRepository: PolicyRepository,
) : ViewModel() {

    private val policy: StateFlow<Policy> = policyRepository.localPolicy()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Policy())

    val pinAvailable: Boolean get() = policy.value.unlockPin.isNotEmpty()

    /** Correct PIN grants a temporary grace window on [pkg]; returns whether it matched. */
    fun submitPin(pkg: String, pin: String): Boolean {
        val current = policy.value
        return if (current.unlockPin.isNotEmpty() && pin == current.unlockPin) {
            lockController.grantGrace(pkg, current.pinGraceMinutes)
            true
        } else false
    }
}
