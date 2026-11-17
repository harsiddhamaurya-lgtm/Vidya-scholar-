package com.example.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

class AuthRepository(
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
) {
    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { instance ->
            trySend(instance.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    fun authStateFlow(scope: CoroutineScope): StateFlow<FirebaseUser?> =
        observeAuthState().stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = auth?.currentUser
        )

    fun signOut() {
        auth?.signOut()
    }
}
