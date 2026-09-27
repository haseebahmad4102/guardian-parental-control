package com.example.guardian.child.ui

import androidx.lifecycle.ViewModel
import com.example.guardian.child.consent.ConsentManager
import com.example.guardian.child.data.DeviceIdentity
import com.example.guardian.child.data.DeviceIdentityStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ChildViewModel @Inject constructor(
    private val consentManager: ConsentManager,
    identityStore: DeviceIdentityStore,
) : ViewModel() {

    val consented: StateFlow<Boolean> = consentManager.consented
    val identity: StateFlow<DeviceIdentity?> = identityStore.identity

    fun grantConsent() = consentManager.grantConsent()
}
