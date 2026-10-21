package com.example.test

import android.Manifest
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.example.test.alarm.AlarmCoordinator
import com.example.test.alarm.AlarmSyncResult
import com.google.android.material.button.MaterialButton
import java.util.concurrent.Executors

/** Describe permission and scheduling results, rather than claiming an alarm was delivered. */
class SettingActivity : AppCompatActivity() {
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var preferences: SettingsPreferences
    private lateinit var status: TextView
    private lateinit var permissionButton: MaterialButton
    private lateinit var exactButton: MaterialButton
    private var refreshing = false
    private var refreshAgain = false
    private var alarmDetails = ""
    private var detailsDialog: androidx.appcompat.app.AlertDialog? = null

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        reconcile()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        applySystemBarInsets()
        ScreenToolbar.bind(this, R.string.settings_page_title, back = true)
        setupBottomNavigation(0)
        findViewById<TextView>(R.id.settings_build).text = getString(
            R.string.compact_build_details, com.example.test.Hero.HeroCatalog.all.size)
        findViewById<MaterialButton>(R.id.settings_progress).setOnClickListener { startActivity(Intent(this, ProgressActivity::class.java)) }
        findViewById<MaterialButton>(R.id.settings_backup).setOnClickListener {
            startActivity(Intent(this, BackupActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.settings_tutorial).setOnClickListener {
            startActivity(Intent(this, TutorialActivity::class.java).putExtra("replay", true))
        }
        findViewById<MaterialButton>(R.id.settings_alarm_details).setOnClickListener {
            detailsDialog?.dismiss()
            detailsDialog = androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.settings_notifications_group)
                .setMessage(alarmDetails.ifBlank { getString(R.string.settings_checking_alarms) })
                .setPositiveButton(android.R.string.ok, null).show()
        }
        preferences = SettingsPreferences(applicationContext)
        status = findViewById(R.id.settings_alarm_status)
        permissionButton = findViewById(R.id.settings_notification_permission)
        exactButton = findViewById(R.id.settings_exact_alarm_permission)
        val sound = findViewById<SwitchCompat>(R.id.switch_sound)
        val alarm = findViewById<SwitchCompat>(R.id.switch_alarm)
        // Restore before listeners: entering Settings does not change the saved preferences.
        sound.isChecked = preferences.soundEnabled
        alarm.isChecked = preferences.alarmEnabled
        sound.setOnCheckedChangeListener { _, enabled ->
            preferences.soundEnabled = enabled
            reconcile()
        }
        alarm.setOnCheckedChangeListener { _, enabled ->
            preferences.alarmEnabled = enabled
            if (enabled && needsNotificationPermission()) {
                requestOrOpenNotificationSettings()
            }
            reconcile()
        }
        permissionButton.setOnClickListener {
            if (needsNotificationPermission()) {
                requestOrOpenNotificationSettings()
            } else {
                openSystemSettings(notificationSettingsIntent())
            }
        }
        exactButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                openSystemSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            }
        }
        findViewById<MaterialButton>(R.id.settings_system_notifications).setOnClickListener {
            openSystemSettings(notificationSettingsIntent())
        }
        findViewById<MaterialButton>(R.id.settings_alarm_preview).setOnClickListener { button ->
            button.isEnabled = false
            AppIo.run(this, { com.example.test.alarm.ExperienceReminder(applicationContext).preview() }) { result ->
                button.isEnabled = true
                val message = when (result.getOrDefault(com.example.test.alarm.ReminderResult.FAILED)) {
                    com.example.test.alarm.ReminderResult.EXACT -> R.string.polish_preview_scheduled
                    com.example.test.alarm.ReminderResult.APPROXIMATE -> R.string.polish_preview_approximate
                    com.example.test.alarm.ReminderResult.DISABLED -> R.string.polish_preview_off
                    com.example.test.alarm.ReminderResult.BLOCKED -> R.string.polish_preview_blocked
                    com.example.test.alarm.ReminderResult.FAILED -> R.string.polish_preview_error
                }
                findViewById<TextView>(R.id.settings_preview_status).apply { setText(message); visibility = android.view.View.VISIBLE }
            }
        }
        findViewById<MaterialButton>(R.id.layout_etc).setOnClickListener {
            startActivity(Intent(this, CreditsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        reconcile()
    }

    private fun requestOrOpenNotificationSettings() {
        val history = getSharedPreferences("notification_permission_ui", MODE_PRIVATE)
        if (needsNotificationPermission() &&
            (!history.getBoolean("requested", false) || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
            history.edit().putBoolean("requested", true).apply()
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openSystemSettings(notificationSettingsIntent())
        }
    }

    private fun needsNotificationPermission() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    private fun reconcile() {
        if (!::preferences.isInitialized || isDestroyed || isFinishing) return
        if (refreshing) {
            refreshAgain = true
            return
        }
        refreshing = true
        status.setText(R.string.settings_checking_alarms)
        val context = applicationContext
        io.execute {
            val result = runCatching { AlarmCoordinator(context).reconcile() }
            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                refreshing = false
                result.fold(onSuccess = ::renderStatus, onFailure = {
                    status.setText(R.string.settings_alarm_sync_failed)
                    updatePermissionButtons()
                })
                // A changed preference while the worker ran must also be reconciled.
                if (refreshAgain) {
                    refreshAgain = false
                    reconcile()
                }
            }
        }
    }

    private fun renderStatus(result: AlarmSyncResult) {
        val lines = mutableListOf<String>()
        if (!preferences.alarmEnabled) {
            lines += getString(R.string.settings_alarms_off)
        } else if (result.notificationsBlocked) {
            lines += getString(R.string.settings_notifications_blocked)
        } else {
            lines += getString(R.string.settings_alarm_count, result.exactScheduled, result.inexactScheduled)
            if (result.exactPermissionMissing) lines += getString(R.string.settings_inexact_notice)
        }
        if (result.errors.isNotEmpty()) lines += getString(R.string.settings_alarm_errors)
        lines += getString(if (preferences.soundEnabled) R.string.settings_sound_on_notice else R.string.settings_sound_off_notice)
        alarmDetails = lines.joinToString("\n\n")
        status.text = when {
            !preferences.alarmEnabled -> getString(R.string.experience_alarms_off)
            result.notificationsBlocked -> getString(R.string.experience_alarm_permission)
            result.errors.isNotEmpty() -> getString(R.string.settings_alarm_sync_failed)
            else -> getString(R.string.settings_alarm_count, result.exactScheduled, result.inexactScheduled)
        }
        updatePermissionButtons()
    }

    private fun updatePermissionButtons() {
        val notificationGranted = !needsNotificationPermission()
        permissionButton.setText(if (notificationGranted) R.string.settings_notification_controls else R.string.settings_allow_notifications)
        permissionButton.visibility = if (notificationGranted) android.view.View.GONE else android.view.View.VISIBLE
        exactButton.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !runCatching { getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }.getOrDefault(false)) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun notificationSettingsIntent(): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
    }

    private fun openSystemSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, R.string.settings_system_screen_unavailable, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        detailsDialog?.dismiss()
        io.shutdown()
        super.onDestroy()
    }
}
