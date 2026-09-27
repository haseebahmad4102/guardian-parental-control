package com.example.guardian.parent.ui.device

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardian.core.model.Command
import com.example.guardian.core.model.Device
import com.example.guardian.core.model.DeviceStatus
import com.example.guardian.core.model.NotificationEvent
import com.example.guardian.core.model.UninstallStatus
import com.example.guardian.core.usage.AggregatedUsage
import com.example.guardian.core.usage.UsageAggregator
import com.example.guardian.core.util.DateKeys
import com.example.guardian.parent.data.CommandRepository
import com.example.guardian.parent.data.DeviceDataRepository
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

enum class UsageRange(val label: String, val days: Int) {
    DAILY("Today", 1),
    WEEKLY("Last 7 days", 7),
}

data class DeviceUiState(
    val device: Device? = null,
    val status: DeviceStatus? = null,
    val range: UsageRange = UsageRange.DAILY,
    val usage: AggregatedUsage = AggregatedUsage(emptyList(), 0L),
    val notifications: List<NotificationEvent> = emptyList(),
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: DeviceDataRepository,
    private val commandRepository: CommandRepository,
) : ViewModel() {

    private val deviceId: String = requireNotNull(savedStateHandle["deviceId"]) {
        "DeviceViewModel requires a deviceId nav argument."
    }

    private val range = MutableStateFlow(UsageRange.DAILY)

    fun setRange(newRange: UsageRange) { range.value = newRange }

    private val _sendingLock = MutableStateFlow(false)
    val sendingLock: StateFlow<Boolean> = _sendingLock.asStateFlow()

    val commands: StateFlow<List<Command>> =
        commandRepository.observeCommands(deviceId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uninstallStatus: StateFlow<UninstallStatus> =
        repository.observeUninstallStatus(deviceId)
            .catch { emit(UninstallStatus.NONE) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UninstallStatus.NONE)

    /** Approve/refuse a pending child uninstall request. */
    fun decideUninstall(allowed: Boolean) {
        viewModelScope.launch { runCatching { commandRepository.decideUninstall(deviceId, allowed) } }
    }

    /** Enqueues a remote-lock command. The child executes it via its live command listener. */
    fun lockDevice() {
        if (_sendingLock.value) return
        _sendingLock.value = true
        viewModelScope.launch {
            runCatching { commandRepository.sendLock(deviceId) }
            _sendingLock.value = false
        }
    }

    /** Enqueues a remote-unlock command, clearing a parent-initiated lock on the child. */
    fun unlockDevice() {
        if (_sendingLock.value) return
        _sendingLock.value = true
        viewModelScope.launch {
            runCatching { commandRepository.sendUnlock(deviceId) }
            _sendingLock.value = false
        }
    }

    val state: StateFlow<DeviceUiState> = combine(
        repository.observeDevice(deviceId).catch { emit(null) },
        repository.observeStatus(deviceId).catch { emit(null) },
        repository.observeUsage(deviceId).catch { emit(emptyList()) },
        repository.observeNotifications(deviceId).catch { emit(emptyList()) },
        range,
    ) { device, status, usageRecords, notifications, selectedRange ->
        val dates = DateKeys.recentDateKeys(selectedRange.days).toSet()
        DeviceUiState(
            device = device,
            status = status,
            range = selectedRange,
            usage = UsageAggregator.aggregate(usageRecords, dates),
            notifications = notifications,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceUiState())
}
