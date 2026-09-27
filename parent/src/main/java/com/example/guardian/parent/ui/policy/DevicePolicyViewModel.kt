package com.example.guardian.parent.ui.policy

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardian.core.model.Policy
import com.example.guardian.parent.data.DeviceDataRepository
import com.example.guardian.parent.data.PolicyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** An app the parent can block or limit (drawn from apps the child has actually used). */
data class AppInfo(val pkg: String, val label: String)

@HiltViewModel
class DevicePolicyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    dataRepository: DeviceDataRepository,
    private val policyRepository: PolicyRepository,
) : ViewModel() {

    private val deviceId: String = requireNotNull(savedStateHandle["deviceId"]) {
        "DevicePolicyViewModel requires a deviceId nav argument."
    }

    val policy: StateFlow<Policy> = policyRepository.observePolicy(deviceId)
        .catch { emit(Policy()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Policy())

    /** Catalog of apps to choose from: every launchable app the child reported, plus anything it
     *  has used or that's already named in the policy (so nothing is ever hidden from the parent). */
    val apps: StateFlow<List<AppInfo>> = combine(
        dataRepository.observeInstalledApps(deviceId).catch { emit(null) },
        dataRepository.observeUsage(deviceId).catch { emit(emptyList()) },
        policy,
    ) { installed, usageRecords, currentPolicy ->
        val labels = LinkedHashMap<String, String>()
        installed?.apps?.forEach { app -> labels.putIfAbsent(app.pkg, app.label.ifBlank { app.pkg }) }
        usageRecords.forEach { record ->
            record.apps.keys.forEach { pkg -> labels.putIfAbsent(pkg, record.labelFor(pkg)) }
        }
        (currentPolicy.blockedApps + currentPolicy.limits.keys).forEach { pkg ->
            labels.putIfAbsent(pkg, pkg)
        }
        labels.map { AppInfo(it.key, it.value) }.sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun save(updated: Policy) {
        if (_saving.value) return
        _saving.value = true
        _saved.value = false
        viewModelScope.launch {
            runCatching { policyRepository.setPolicy(deviceId, updated) }
                .onSuccess { _saved.value = true }
            _saving.value = false
        }
    }

    fun consumeSaved() { _saved.value = false }
}
