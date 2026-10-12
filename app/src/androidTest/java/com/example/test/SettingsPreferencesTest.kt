package com.example.test

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPreferencesTest {
    private lateinit var context: Context
    private lateinit var preferenceName: String

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        preferenceName = "settings_test_${UUID.randomUUID()}"
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(preferenceName)
    }

    @Test
    fun newInstallationPreservesExistingEnabledDefaults() {
        val settings = newSettings()

        assertTrue(settings.soundEnabled)
        assertTrue(settings.alarmEnabled)
    }

    @Test
    fun bothDisabledPreferencesSurviveRecreatingTheStore() {
        newSettings().apply {
            soundEnabled = false
            alarmEnabled = false
        }

        val restored = newSettings()
        assertFalse(restored.soundEnabled)
        assertFalse(restored.alarmEnabled)
    }

    @Test
    fun changingOnePreferenceDoesNotChangeTheOther() {
        val settings = newSettings()
        settings.soundEnabled = false

        assertFalse(newSettings().soundEnabled)
        assertTrue(newSettings().alarmEnabled)

        settings.alarmEnabled = false
        settings.soundEnabled = true

        assertTrue(newSettings().soundEnabled)
        assertFalse(newSettings().alarmEnabled)
    }

    private fun newSettings() = SettingsPreferences(context, preferenceName)
}
