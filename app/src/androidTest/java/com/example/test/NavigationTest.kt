package com.example.test

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/** Device regressions for screen routing and task-root preservation. */
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @org.junit.Before fun returningUser() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences(TutorialActivity.PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("completed", true).commit()
    }

    @Test
    fun routineButtonOpensRoutineAndBackReturnsToSameHome() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var originalHome: MainActivity
            scenario.onActivity { originalHome = it }

            onView(withId(R.id.nav_routine)).perform(click())
            onView(withId(R.id.rv_routine_list)).check(matches(isDisplayed()))
            pressBack()

            scenario.onActivity { activity ->
                assertSame(originalHome, activity)
                assertEquals(R.id.nav_home, activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId)
            }
        }
    }

    @Test
    fun tabsReturnToExistingHomeWithoutDuplicateRoot() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var originalHome: MainActivity
            scenario.onActivity { originalHome = it }

            onView(withId(R.id.nav_hero)).perform(click())
            onView(withId(R.id.heroRecyclerView)).check(matches(isDisplayed()))
            onView(withId(R.id.nav_home)).perform(click())
            androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext)
            onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.nav_settings)).perform(click())
            onView(withId(R.id.switch_sound)).check(matches(isDisplayed()))
            onView(withId(R.id.nav_home)).perform(click())
            onView(withId(R.id.nav_home)).check(matches(isDisplayed()))

            scenario.onActivity { assertSame(originalHome, it) }
        }
    }

    @Test
    fun myroomOpensShopAndBackReturnsToExistingHome() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var originalHome: MainActivity
            scenario.onActivity { activity ->
                originalHome = activity
            }
            onView(withId(R.id.nav_myroom)).perform(click())
            onView(withId(R.id.shop_title)).check(matches(isDisplayed()))
            pressBack()
            scenario.onActivity { activity ->
                assertSame(originalHome, activity)
                assertEquals(R.id.nav_home, activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId)
            }
        }
    }

    @Test
    fun navigatingToCurrentSettingsDoesNotRecreateIt() {
        ActivityScenario.launch(SettingActivity::class.java).use { scenario ->
            lateinit var originalSettings: SettingActivity
            scenario.onActivity { activity ->
                originalSettings = activity
                activity.navigateTo(SettingActivity::class.java)
                assertFalse(activity.isFinishing)
            }
            scenario.onActivity { assertSame(originalSettings, it) }
        }
    }
}
