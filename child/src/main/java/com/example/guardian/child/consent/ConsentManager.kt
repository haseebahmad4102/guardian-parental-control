package com.example.guardian.child.consent

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists whether the device holder has seen and accepted the monitoring-consent screen.
 *
 * Consent is legally and ethically required on first launch (a parent may lawfully monitor
 * their own minor child on a device they own, but the screen must be shown regardless).
 * This flag also gates whether the app runs in a fully transparent mode.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _consented = MutableStateFlow(prefs.getBoolean(KEY_CONSENTED, false))
    val consented: StateFlow<Boolean> = _consented.asStateFlow()

    fun grantConsent() {
        prefs.edit().putBoolean(KEY_CONSENTED, true)
            .putLong(KEY_CONSENTED_AT, System.currentTimeMillis())
            .apply()
        _consented.value = true
    }

    val consentedAt: Long get() = prefs.getLong(KEY_CONSENTED_AT, 0L)

    private companion object {
        const val PREFS = "guardian_consent"
        const val KEY_CONSENTED = "consented"
        const val KEY_CONSENTED_AT = "consented_at"
    }
}
