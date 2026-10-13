package com.example.test

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.test.Hero.HeroCatalog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the actual launcher layout; requires an Android device to execute. */
@RunWith(AndroidJUnit4::class)
class TodayScreenTest {
    @Test fun launcherShowsHeroPickerAndEightBlockPreviewWithoutAdoptingAnything() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.today_hero_card).visibility)
                assertEquals(30, activity.findViewById<Spinner>(R.id.today_hero_picker).count)
                assertTrue(activity.findViewById<Button>(R.id.today_start_day).isEnabled)
                activity.findViewById<Button>(R.id.today_preview_toggle).performClick()
                val preview = activity.findViewById<LinearLayout>(R.id.today_day_preview)
                assertEquals(View.VISIBLE, preview.visibility)
                assertEquals(8, preview.childCount)
                assertNotNull(activity.findViewById<View>(R.id.timeline_progress))
                assertNotNull(activity.findViewById<View>(R.id.rv_routine_list))
            }
        }
    }
    @Test fun recreationPreservesChosenPersonAndExpandedPreview() {
        val index = HeroCatalog.all.indexOfFirst { it.id == "newton" }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Spinner>(R.id.today_hero_picker).setSelection(index)
                activity.findViewById<Button>(R.id.today_preview_toggle).performClick()
            }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(index, activity.findViewById<Spinner>(R.id.today_hero_picker).selectedItemPosition)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.today_day_preview).visibility)
                assertEquals(8, activity.findViewById<LinearLayout>(R.id.today_day_preview).childCount)
            }
        }
    }
}
