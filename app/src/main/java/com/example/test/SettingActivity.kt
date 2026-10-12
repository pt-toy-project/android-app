package com.example.test

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        applySystemBarInsets()
        setupBottomNavigation(R.id.nav_option)

        val preferences = SettingsPreferences(this)
        val switchSound = findViewById<SwitchCompat>(R.id.switch_sound)
        val switchAlarm = findViewById<SwitchCompat>(R.id.switch_alarm)
        val layoutEtc = findViewById<View>(R.id.layout_etc)

        // Restore preferences before attaching listeners so opening the screen does not write values.
        switchSound.isChecked = preferences.soundEnabled
        switchAlarm.isChecked = preferences.alarmEnabled

        switchSound.setOnCheckedChangeListener { _, isChecked ->
            preferences.soundEnabled = isChecked
            showPreferenceSaved(R.string.settings_sound, isChecked)
        }

        switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            preferences.alarmEnabled = isChecked
            showPreferenceSaved(R.string.settings_alarm, isChecked)
        }

        layoutEtc.setOnClickListener {
            Toast.makeText(this, R.string.settings_other_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPreferenceSaved(name: Int, enabled: Boolean) {
        val message = if (enabled) R.string.settings_enabled_saved else R.string.settings_disabled_saved
        Toast.makeText(this, getString(message, getString(name)), Toast.LENGTH_SHORT).show()
    }
}
