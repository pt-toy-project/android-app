package com.example.test

import android.accounts.Account
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.test.data.BackupStore
import com.example.test.data.GoogleDriveBackup
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.Scope

class BackupActivity : AppCompatActivity() {
    private var busy = false
    private var pendingCloud: String? = null
    private var restoreDialog: AlertDialog? = null
    private lateinit var status: TextView
    private val exportFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) work({
            val bytes = BackupStore(applicationContext).export()
            requireNotNull(contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
        }) { status.setText(R.string.backup_saved) }
    }
    private val importFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) work({ requireNotNull(contentResolver.openInputStream(uri)).use(BackupStore::readLimited) }) { preview(it) }
    }
    private val resolution = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { response ->
        runCatching { Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(requireNotNull(response.data)) }
            .onSuccess(::authorized).onFailure { stopWith(R.string.backup_google_failed) }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_backup)
        applySystemBarInsets()
        ScreenToolbar.bind(this, R.string.backup_title, back = true)
        status = findViewById(R.id.backup_status)
        pendingCloud = state?.getString("pendingCloud")
        renderAccount()
        if (pendingCloud != null) setBusy(true)
        findViewById<View>(R.id.backup_sign_in).setOnClickListener {
            if (busy) return@setOnClickListener
            setBusy(true)
            GoogleAccountSession.signIn(this) { success ->
                setBusy(false); renderAccount()
                status.setText(if (success) R.string.account_selected else R.string.account_unavailable)
            }
        }
        findViewById<View>(R.id.backup_sign_out).setOnClickListener {
            if (busy) return@setOnClickListener
            setBusy(true)
            GoogleAccountSession.signOut(this) { success ->
                setBusy(false); renderAccount()
                status.setText(if (success) R.string.account_signed_out else R.string.account_unavailable)
            }
        }
        findViewById<View>(R.id.backup_google_upload).setOnClickListener { authorize("upload") }
        findViewById<View>(R.id.backup_google_restore).setOnClickListener { authorize("restore") }
        findViewById<View>(R.id.backup_export).setOnClickListener { if (!busy) exportFile.launch("위인의하루-${System.currentTimeMillis()}.heroday.json") }
        findViewById<View>(R.id.backup_import).setOnClickListener { if (!busy) importFile.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
        findViewById<View>(R.id.backup_undo).apply {
            visibility = if (BackupStore(applicationContext).hasSafetyBackup()) View.VISIBLE else View.GONE
            setOnClickListener { work({ BackupStore(applicationContext).safetyBackup() }) { preview(it) } }
        }
    }

    private fun renderAccount() {
        val session = GoogleAccountSession(this)
        val available = GoogleAccountSession.available()
        findViewById<View>(R.id.backup_sign_in).visibility = if (available && session.email == null) View.VISIBLE else View.GONE
        listOf(R.id.backup_google_upload, R.id.backup_google_restore).forEach {
            findViewById<View>(it).visibility = if (available) View.VISIBLE else View.GONE
        }
        findViewById<TextView>(R.id.backup_account).text = session.email ?: getString(R.string.account_not_signed_in)
        findViewById<View>(R.id.backup_sign_out).visibility = if (session.email == null) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.backup_google_notice).setText(
            if (GoogleAccountSession.available()) R.string.backup_cloud_note else R.string.account_not_configured)
    }

    private fun authorize(operation: String) {
        if (busy) return
        val email = GoogleAccountSession(this).email
        if (!GoogleAccountSession.available() || email == null) { status.setText(R.string.backup_sign_in_first); return }
        setBusy(true); pendingCloud = operation
        val request = AuthorizationRequest.builder().setAccount(Account(email, "com.google"))
            .setRequestedScopes(listOf(Scope(GoogleDriveBackup.SCOPE))).build()
        Identity.getAuthorizationClient(this).authorize(request).addOnSuccessListener(this, ::authorized)
            .addOnFailureListener(this) { stopWith(R.string.backup_google_failed) }
    }

    private fun authorized(result: AuthorizationResult) {
        if (result.hasResolution()) {
            val pending = result.pendingIntent ?: run { stopWith(R.string.backup_google_failed); return }
            resolution.launch(IntentSenderRequest.Builder(pending.intentSender).build()); return
        }
        val authorizedEmail = result.toGoogleSignInAccount()?.email
        if (authorizedEmail != null && authorizedEmail != GoogleAccountSession(this).email) {
            stopWith(R.string.backup_google_failed); return
        }
        val token = result.accessToken
        if (token.isNullOrBlank() || GoogleDriveBackup.SCOPE !in result.grantedScopes) { stopWith(R.string.backup_google_failed); return }
        val operation = pendingCloud
        pendingCloud = null
        // Tokens are passed directly to a short worker and never persisted.
        if (operation == "upload") work({ GoogleDriveBackup(token).upload(BackupStore(applicationContext).export()) }, alreadyBusy = true) {
            getSharedPreferences("backup_status", Context.MODE_PRIVATE).edit().putLong("last_backup", System.currentTimeMillis()).apply()
            status.setText(R.string.backup_cloud_saved)
        } else if (operation == "restore") work({ GoogleDriveBackup(token).downloadLatest() }, alreadyBusy = true) { preview(it) }
        else stopWith(R.string.backup_google_failed)
    }

    private fun preview(bytes: ByteArray) {
        work({ BackupStore(applicationContext).preview(bytes) }) { summary ->
            val date = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(summary.createdAt))
            restoreDialog?.dismiss()
            restoreDialog = AlertDialog.Builder(this).setTitle(R.string.backup_restore_review)
                .setMessage(getString(R.string.backup_restore_summary, date, summary.routines, summary.records, summary.balance))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.backup_restore_confirm) { _, _ ->
                    work({
                        val coordinator = com.example.test.alarm.AlarmCoordinator(applicationContext)
                        coordinator.cancelAll()
                        try {
                            BackupStore(applicationContext).restore(bytes)
                            // A restored ID may refer to a different plan; an old timer must not follow it.
                            com.example.test.alarm.ExperienceReminder(applicationContext).cancelFocus()
                            com.example.test.focus.FocusTimerStore(applicationContext).clear()
                        }
                        finally { runCatching { coordinator.reconcile() } }
                    }) { status.setText(R.string.backup_restored); findViewById<View>(R.id.backup_undo).visibility = View.VISIBLE }
                }.show()
        }
    }

    private fun <T> work(action: () -> T, alreadyBusy: Boolean = false, success: (T) -> Unit) {
        if (busy && !alreadyBusy) return
        setBusy(true)
        AppIo.run(this, action) { result ->
            setBusy(false)
            result.onSuccess(success).onFailure { status.setText(R.string.backup_failed) }
        }
    }
    private fun setBusy(value: Boolean) {
        busy = value
        listOf(R.id.backup_sign_in, R.id.backup_sign_out, R.id.backup_google_upload, R.id.backup_google_restore,
            R.id.backup_export, R.id.backup_import, R.id.backup_undo).forEach { findViewById<View>(it).isEnabled = !value }
        if (value) status.setText(R.string.backup_working)
    }
    private fun stopWith(message: Int) { pendingCloud = null; setBusy(false); status.setText(message) }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("pendingCloud", pendingCloud); super.onSaveInstanceState(outState) }
    override fun onDestroy() { restoreDialog?.dismiss(); super.onDestroy() }
}
