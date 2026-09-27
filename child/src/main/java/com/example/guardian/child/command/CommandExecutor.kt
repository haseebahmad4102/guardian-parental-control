package com.example.guardian.child.command

import android.content.Context
import android.content.Intent
import com.example.guardian.child.admin.DeviceAdminManager
import com.example.guardian.child.data.PolicyRepository
import com.example.guardian.child.enforcement.GuardianAccessibilityService
import com.example.guardian.child.enforcement.LockController
import com.example.guardian.child.enforcement.LockScreenActivity
import com.example.guardian.core.model.Command
import com.example.guardian.core.model.CommandType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CommandResult {
    data object Success : CommandResult
    data class Failure(val message: String) : CommandResult
}

/**
 * Executes a single [Command] on the child device. LOCK raises the Guardian lock screen (and turns
 * the screen off via Device Admin if available); UNLOCK clears it. Unknown types fail loudly.
 */
@Singleton
class CommandExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adminManager: DeviceAdminManager,
    private val policyRepository: PolicyRepository,
    private val lockController: LockController,
) {
    suspend fun execute(command: Command): CommandResult { return when (command.commandType) {
        CommandType.LOCK -> lock()
        CommandType.UNLOCK -> {
            lockController.unlock()
            CommandResult.Success
        }
        CommandType.SCREENSHOT -> {
            val service = GuardianAccessibilityService.instance
                ?: return CommandResult.Failure("Screen access isn't enabled (turn on Accessibility on the child).")
            when (val error = service.captureAndUpload()) {
                null -> CommandResult.Success
                else -> CommandResult.Failure(error)
            }
        }
        CommandType.TAP -> {
            val service = GuardianAccessibilityService.instance
                ?: return CommandResult.Failure("Screen control isn't enabled (turn on Accessibility).")
            val fx = command.payload["fx"]?.toFloatOrNull()
            val fy = command.payload["fy"]?.toFloatOrNull()
            if (fx == null || fy == null) return CommandResult.Failure("Malformed tap coordinates.")
            service.performTap(fx, fy)
            CommandResult.Success
        }
        CommandType.OPEN_APP -> openApp(command.payload["package"])
        // Parent edits are written to policies/current and picked up by the live listener; this
        // just nudges an immediate re-sync (e.g. when pushed via FCM while backgrounded).
        CommandType.REFRESH_POLICY,
        CommandType.BLOCK,
        CommandType.SET_LIMIT,
        CommandType.SET_BUDGET -> {
            policyRepository.syncOnce()
            CommandResult.Success
        }
        CommandType.UNKNOWN ->
            CommandResult.Failure("Unrecognized command type: '${command.type}'.")
    } }

    private fun openApp(pkg: String?): CommandResult {
        if (pkg.isNullOrBlank()) return CommandResult.Failure("No app specified.")
        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return CommandResult.Failure("That app can't be launched.")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(launch); CommandResult.Success }
            .getOrElse { CommandResult.Failure(it.message ?: "Couldn't open the app.") }
    }

    private fun lock(): CommandResult {
        // Assert the Guardian lock regardless of Device Admin (the lock screen doesn't need it)...
        lockController.lock()
        runCatching { context.startActivity(LockScreenActivity.intent(context)) }
        // ...and, if Device Admin is active, also turn the screen off immediately.
        if (adminManager.isActive()) {
            runCatching { adminManager.lockNow() }
        }
        return CommandResult.Success
    }
}
