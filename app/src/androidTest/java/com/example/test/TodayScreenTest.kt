package com.example.test

import android.view.View
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Device-only regressions complement the executable Robolectric screen tests. */
@RunWith(AndroidJUnit4::class)
class TodayScreenTest {
    @org.junit.Before fun returningUser() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences(TutorialActivity.PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("completed", true).commit()
    }

    @Test fun launcherShowsProgressTimelineAndExplicitStartRoute() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById<View>(R.id.screen_toolbar))
                assertNotNull(activity.findViewById<View>(R.id.timeline_progress))
                assertNotNull(activity.findViewById<View>(R.id.rv_routine_list))
                assertTrue(activity.findViewById<Button>(R.id.personal_add).isEnabled)
                assertNotNull(activity.findViewById<View>(R.id.timeline_next_complete))
            }
        }
    }
    @Test fun recreationPreservesDedicatedTodayAndPrimaryRoutes() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.recreate()
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById<View>(R.id.timeline_empty_card))
                assertTrue(activity.findViewById<Button>(R.id.personal_manage).isEnabled)
                assertEquals(R.id.nav_home, activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView).selectedItemId)
            }
        }
    }
}
