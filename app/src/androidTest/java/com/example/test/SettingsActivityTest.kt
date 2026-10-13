package com.example.test

import androidx.appcompat.widget.SwitchCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Device-only regressions: preference restoration and the bundled citations screen. */
@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {
    @Test
    fun soundChoiceSurvivesActivityRecreation() {
        val preferences = SettingsPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
        val original = preferences.soundEnabled
        try {
            ActivityScenario.launch(SettingActivity::class.java).use { scenario ->
                scenario.onActivity { it.findViewById<SwitchCompat>(R.id.switch_sound).isChecked = !original }
                assertEquals(!original, preferences.soundEnabled)
                scenario.recreate()
                scenario.onActivity { assertEquals(!original, it.findViewById<SwitchCompat>(R.id.switch_sound).isChecked) }
            }
        } finally {
            preferences.soundEnabled = original
        }
    }

    @Test
    fun citationsOpenFromSettingsAndBackReturnsToSettings() {
        ActivityScenario.launch(SettingActivity::class.java).use {
            onView(withId(R.id.layout_etc)).perform(scrollTo(), click())
            onView(withId(R.id.credits_webview)).check(matches(isDisplayed()))
            pressBack()
            onView(withId(R.id.switch_sound)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }
}
