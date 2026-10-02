package com.myday.litu.core.sync

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.myday.litu.core.analytics.AnalyticsEvent
import com.myday.litu.core.analytics.AnalyticsLogger
import com.myday.litu.core.billing.EntitlementRepository
import com.myday.litu.core.config.FirebaseAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAccountRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backup: BackupRepository,
    private val entitlements: EntitlementRepository,
    private val analytics: AnalyticsLogger,
) : AccountRepository {

    private val auth: FirebaseAuth? by lazy {
        if (FirebaseAvailability.isConfigured(context)) FirebaseAuth.getInstance() else null
    }

    private val mutableState = MutableStateFlow<AccountState>(AccountState.SignedOut)
    override val state: StateFlow<AccountState> = mutableState.asStateFlow()

    private fun publish(user: FirebaseUser?) {
        mutableState.value = when {
            auth == null -> AccountState.Unavailable
            user == null -> AccountState.SignedOut
            user.isAnonymous -> AccountState.Anonymous(user.uid)
            else -> AccountState.Linked(user.uid, user.email)
        }
    }

    override suspend fun ensureSignedIn() {
        val a = auth ?: return publish(null)
        val user = a.currentUser ?: runCatching { a.signInAnonymously().await().user }
            .onFailure { Log.w(TAG, "Anonymous sign-in failed (offline?); will retry next launch", it) }
            .getOrNull()
        publish(user)
        user?.let { entitlements.identify(it.uid) }
    }

    // Firebase can fail in many ways here; every failure becomes a message the user can act on.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun linkGoogle(activity: Activity): AccountResult {
        val a = auth ?: return AccountResult.Failed("Backup is not available in this build.")
        val idToken = when (val r = googleIdToken(activity)) {
            is TokenResult.Token -> r.value
            TokenResult.Cancelled -> return AccountResult.Cancelled
            is TokenResult.Error -> return AccountResult.Failed(r.message)
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return try {
            val current = a.currentUser ?: a.signInAnonymously().await().user!!
            val linked = current.linkWithCredential(credential).await().user
            publish(linked)
            analytics.log(AnalyticsEvent.BackupLinked)
            backup.backupNow()
            AccountResult.Success
        } catch (e: FirebaseAuthUserCollisionException) {
            // This Google account already has a backup (for example from an old phone): restore it.
            val user = a.signInWithCredential(credential).await().user
            publish(user)
            user?.let { entitlements.identify(it.uid) }
            backup.restore()
            AccountResult.Restored
        } catch (e: Exception) {
            Log.w(TAG, "Google link failed", e)
            AccountResult.Failed("Could not link your Google account. Check your connection and try again.")
        }
    }

    override suspend fun signOut() {
        val a = auth ?: return
        a.signOut()
        ensureSignedIn()
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun deleteAccount(): AccountResult {
        val a = auth ?: return AccountResult.Success
        val user = a.currentUser ?: return AccountResult.Success
        return try {
            backup.deleteRemote(user.uid)
            user.delete().await()
            publish(null)
            ensureSignedIn()
            AccountResult.Success
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            AccountResult.Failed("For your security, sign in with Google again, then delete your data.")
        } catch (e: Exception) {
            Log.w(TAG, "Account deletion failed", e)
            AccountResult.Failed("Could not delete your online data. Check your connection and try again.")
        }
    }

    private sealed interface TokenResult {
        data class Token(val value: String) : TokenResult
        data object Cancelled : TokenResult
        data class Error(val message: String) : TokenResult
    }

    @SuppressLint("DiscouragedApi")
    @Suppress("SwallowedException") // Cancellation is an expected outcome, not an error.
    private suspend fun googleIdToken(activity: Activity): TokenResult {
        // Generated from google-services.json by the Google Services plugin.
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId == 0) return TokenResult.Error("Google sign-in is not set up for this build.")
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(resId))
            .build()
        return try {
            val response = CredentialManager.create(activity)
                .getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
            val cred = response.credential
            if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                TokenResult.Token(GoogleIdTokenCredential.createFrom(cred.data).idToken)
            } else {
                TokenResult.Error("Unexpected sign-in response.")
            }
        } catch (e: GetCredentialCancellationException) {
            TokenResult.Cancelled
        } catch (e: GetCredentialException) {
            TokenResult.Error(e.message ?: "Google sign-in failed.")
        }
    }

    private companion object {
        const val TAG = "Account"
    }
}
