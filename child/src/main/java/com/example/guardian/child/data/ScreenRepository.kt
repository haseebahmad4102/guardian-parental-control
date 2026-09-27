package com.example.guardian.child.data

import com.example.guardian.core.data.FirestorePaths
import com.example.guardian.core.model.ScreenFrame
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Uploads captured screen frames so the parent can view (and control) the screen. */
@Singleton
class ScreenRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val identityStore: DeviceIdentityStore,
) {
    suspend fun uploadFrame(jpegBase64: String, width: Int, height: Int) {
        val deviceId = identityStore.current?.deviceId ?: return
        firestore.document(FirestorePaths.screenFrame(deviceId))
            .set(
                ScreenFrame(
                    jpeg = jpegBase64,
                    width = width,
                    height = height,
                    capturedAt = System.currentTimeMillis(),
                )
            )
            .await()
    }
}
