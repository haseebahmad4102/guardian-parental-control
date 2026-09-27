package com.example.guardian.child.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** This device's pairing identity once it has been linked to a parent. Null until paired. */
data class DeviceIdentity(
    val deviceId: String,
    val parentUid: String,
    val childName: String,
)

/**
 * Persists the child device's pairing identity so it survives restarts. Later phases read
 * [current] to know which Firestore paths to write usage/notifications under.
 */
@Singleton
class DeviceIdentityStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _identity = MutableStateFlow(readIdentity())
    val identity: StateFlow<DeviceIdentity?> = _identity.asStateFlow()

    val current: DeviceIdentity? get() = _identity.value
    val isPaired: Boolean get() = _identity.value != null

    fun save(identity: DeviceIdentity) {
        prefs.edit()
            .putString(KEY_DEVICE_ID, identity.deviceId)
            .putString(KEY_PARENT_UID, identity.parentUid)
            .putString(KEY_CHILD_NAME, identity.childName)
            .apply()
        _identity.value = identity
    }

    private fun readIdentity(): DeviceIdentity? {
        val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: return null
        val parentUid = prefs.getString(KEY_PARENT_UID, null) ?: return null
        val childName = prefs.getString(KEY_CHILD_NAME, "") ?: ""
        return DeviceIdentity(deviceId, parentUid, childName)
    }

    private companion object {
        const val PREFS = "guardian_identity"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_PARENT_UID = "parent_uid"
        const val KEY_CHILD_NAME = "child_name"
    }
}
