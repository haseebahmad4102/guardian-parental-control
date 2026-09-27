package com.example.guardian.child.enforcement

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists whether the device is currently under a parent-initiated Guardian lock, plus temporary
 * per-app "grace" windows granted when the correct PIN is entered on a limit block. Survives
 * reboots so the lock re-asserts itself.
 */
@Singleton
class LockController @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _locked = MutableStateFlow(prefs.getBoolean(KEY_LOCKED, false))
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    val isLocked: Boolean get() = _locked.value

    fun lock() {
        prefs.edit().putBoolean(KEY_LOCKED, true).apply()
        _locked.value = true
    }

    fun unlock() {
        prefs.edit().putBoolean(KEY_LOCKED, false).apply()
        _locked.value = false
    }

    /** Grants [minutes] of access to [pkg] (used after a correct PIN on a limit block). */
    fun grantGrace(pkg: String, minutes: Long) {
        prefs.edit().putLong(graceKey(pkg), System.currentTimeMillis() + minutes * 60_000L).apply()
    }

    fun isInGrace(pkg: String): Boolean =
        System.currentTimeMillis() < prefs.getLong(graceKey(pkg), 0L)

    private fun graceKey(pkg: String) = "grace_$pkg"

    private companion object {
        const val PREFS = "guardian_lock"
        const val KEY_LOCKED = "locked"
    }
}
