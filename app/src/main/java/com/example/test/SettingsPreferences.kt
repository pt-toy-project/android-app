package com.example.test

import android.content.Context
import androidx.core.content.edit

/** Global notification choices consumed by AlarmCoordinator; Android channel settings also apply. */
class SettingsPreferences(context: Context, fileName: String = "app_settings") {
    private val preferences = context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    var soundEnabled: Boolean
        get() = preferences.getBoolean("sound_enabled", true)
        set(value) {
            preferences.edit { putBoolean("sound_enabled", value) }
        }

    var alarmEnabled: Boolean
        get() = preferences.getBoolean("alarm_enabled", true)
        set(value) {
            preferences.edit { putBoolean("alarm_enabled", value) }
        }
}
