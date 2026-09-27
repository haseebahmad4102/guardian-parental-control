package com.example.guardian.parent.ui.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardian.core.model.CommandStatus
import com.example.guardian.core.model.CommandType
import com.example.guardian.core.model.InstalledApp
import com.example.guardian.core.model.ScreenFrame
import com.example.guardian.parent.data.CommandRepository
import com.example.guardian.parent.data.DeviceDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LIVE_INTERVAL_MILLIS = 2_500L
private val SCREEN_COMMANDS = setOf(CommandType.SCREENSHOT, CommandType.TAP, CommandType.OPEN_APP)

@HiltViewModel
class ScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    dataRepository: DeviceDataRepository,
    private val commandRepository: CommandRepository,
) : ViewModel() {

    private val deviceId: String = requireNotNull(savedStateHandle["deviceId"]) {
        "ScreenViewModel requires a deviceId nav argument."
    }

    val frame: StateFlow<ScreenFrame?> =
        dataRepository.observeScreenFrame(deviceId)
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val apps: StateFlow<List<InstalledApp>> =
        dataRepository.observeInstalledApps(deviceId)
            .map { it?.apps.orEmpty() }
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * A live status line for the most recent screen command, so a blank view is diagnosable:
     * "pending" = child service not draining commands (likely killed by battery optimization),
     * "delivered" = executing, "executed" = captured/uploaded ok, "failed — …" = shows why.
     */
    val commandStatus: StateFlow<String?> =
        commandRepository.observeCommands(deviceId)
            .map { commands ->
                val c = commands.firstOrNull {
                    it.commandType in SCREEN_COMMANDS
                } ?: return@map null
                buildString {
                    append(c.type).append(": ").append(c.status.lowercase())
                    if (c.commandStatus == CommandStatus.FAILED && !c.error.isNullOrBlank()) {
                        append(" — ").append(c.error)
                    }
                }
            }
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _live = MutableStateFlow(false)
    val live: StateFlow<Boolean> = _live.asStateFlow()

    private var liveJob: Job? = null

    init { requestScreenshot() }

    fun requestScreenshot() {
        viewModelScope.launch { runCatching { commandRepository.sendScreenshot(deviceId) } }
    }

    fun toggleLive() {
        _live.value = !_live.value
        liveJob?.cancel()
        if (_live.value) {
            liveJob = viewModelScope.launch {
                while (isActive) {
                    runCatching { commandRepository.sendScreenshot(deviceId) }
                    delay(LIVE_INTERVAL_MILLIS)
                }
            }
        }
    }

    /** Sends a tap at fractional coords, then requests a fresh frame to reflect the result. */
    fun tap(fx: Float, fy: Float) {
        viewModelScope.launch {
            runCatching { commandRepository.sendTap(deviceId, fx, fy) }
            delay(700)
            runCatching { commandRepository.sendScreenshot(deviceId) }
        }
    }

    fun openApp(pkg: String) {
        viewModelScope.launch {
            runCatching { commandRepository.sendOpenApp(deviceId, pkg) }
            delay(1200)
            runCatching { commandRepository.sendScreenshot(deviceId) }
        }
    }

    override fun onCleared() {
        liveJob?.cancel()
        super.onCleared()
    }
}
