package com.example.guardian.parent.data

import com.example.guardian.core.data.FirestorePaths
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Result of an auth attempt: either the signed-in user or a human-readable error message. */
sealed interface AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult
    data class Error(val message: String) : AuthResult
}

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Emits the current user (or null) and re-emits on every sign-in/sign-out. */
    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): AuthResult = runCatching {
        val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: return AuthResult.Error("Sign-in returned no user.")
        AuthResult.Success(user)
    }.getOrElse { AuthResult.Error(it.toFriendlyMessage()) }

    suspend fun signUp(email: String, password: String): AuthResult = runCatching {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: return AuthResult.Error("Sign-up returned no user.")
        // Create the parent's root document. merge() is idempotent if it already exists.
        firestore.collection(FirestorePaths.USERS)
            .document(user.uid)
            .set(
                mapOf(
                    "email" to (user.email ?: email.trim()),
                    "createdAt" to System.currentTimeMillis(),
                )
            )
            .await()
        AuthResult.Success(user)
    }.getOrElse { AuthResult.Error(it.toFriendlyMessage()) }

    fun signOut() = auth.signOut()

    private fun Throwable.toFriendlyMessage(): String = when (this) {
        is FirebaseAuthWeakPasswordException -> "Password is too weak (use at least 6 characters)."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthInvalidUserException -> "No account found for that email."
        is FirebaseAuthUserCollisionException -> "An account already exists for that email."
        else -> message ?: "Something went wrong. Check your connection and try again."
    }
}
