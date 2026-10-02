package com.myday.litu.core.sync

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

sealed interface AccountState {
    /** Firebase is not configured for this build, so backup is unavailable. */
    data object Unavailable : AccountState
    data object SignedOut : AccountState
    data class Anonymous(val uid: String) : AccountState
    data class Linked(val uid: String, val email: String?) : AccountState
}

sealed interface AccountResult {
    data object Success : AccountResult
    data object Restored : AccountResult
    data object Cancelled : AccountResult
    data class Failed(val message: String) : AccountResult
}

interface AccountRepository {
    val state: StateFlow<AccountState>

    /** Signs in anonymously on first launch so every install has a uid (spec section 12.2). */
    suspend fun ensureSignedIn()

    /** "Back up my progress": links a Google account, or restores its backup if it already has one. */
    suspend fun linkGoogle(activity: Activity): AccountResult
    suspend fun signOut()

    /** Deletes the Firestore backup and the Auth user. Local data is cleared by the caller. */
    suspend fun deleteAccount(): AccountResult
}
