package com.example.test

import android.content.Context
import android.os.CancellationSignal
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.ClearCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Only display information is cached. ID/access tokens are never stored, logged or backed up. */
class GoogleAccountSession(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("google_account", Context.MODE_PRIVATE)
    val email: String? get() = prefs.getString("email", null)
    val name: String? get() = prefs.getString("name", null)
    fun clear() { check(prefs.edit().clear().commit()) }

    companion object {
        fun available(): Boolean = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

        fun signIn(activity: AppCompatActivity, finished: (Boolean) -> Unit) {
            if (!available()) { finished(false); return }
            val cancellation = CancellationSignal()
            activity.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) { cancellation.cancel() }
            })
            val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            CredentialManager.create(activity).getCredentialAsync(activity, request, cancellation,
                ContextCompat.getMainExecutor(activity), object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                    override fun onResult(result: GetCredentialResponse) {
                        if (activity.isDestroyed || activity.isFinishing) return
                        val saved = runCatching {
                            require(result.credential is CustomCredential && result.credential.type in setOf(
                                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
                                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL))
                            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                            require(credential.id.isNotBlank())
                            // The cached name is not proof of a live session. Drive operations reauthorize.
                            check(activity.getSharedPreferences("google_account", Context.MODE_PRIVATE).edit()
                                .putString("email", credential.id).putString("name", credential.displayName).commit())
                        }.isSuccess
                        finished(saved)
                    }
                    override fun onError(e: GetCredentialException) {
                        if (!activity.isDestroyed && !activity.isFinishing) finished(false)
                    }
                })
        }

        fun signOut(activity: AppCompatActivity, finished: (Boolean) -> Unit) {
            CredentialManager.create(activity).clearCredentialStateAsync(ClearCredentialStateRequest(), null,
                ContextCompat.getMainExecutor(activity), object : CredentialManagerCallback<Void?, ClearCredentialException> {
                    override fun onResult(result: Void?) {
                        val cleared = runCatching { GoogleAccountSession(activity).clear() }.isSuccess
                        if (!activity.isDestroyed) finished(cleared)
                    }
                    override fun onError(e: ClearCredentialException) {
                        if (!activity.isDestroyed) finished(false)
                    }
                })
        }
    }
}
