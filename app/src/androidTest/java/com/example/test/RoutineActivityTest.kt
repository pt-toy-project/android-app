package com.example.test

import android.content.Intent
import android.view.View
import android.widget.DatePicker
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.recyclerview.widget.RecyclerView
import com.example.test.Hero.HeroCatalog
import com.example.test.Routine.RoutineDate
import com.example.test.Routine.RoutineRepository
import com.example.test.Routine.RoutineScope
import com.example.test.Routine.RoutineCatalog
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Compiled on the host; requires an Android device to execute. Never clears completion records. */
@RunWith(AndroidJUnit4::class)
class RoutineActivityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var previousHero: String
    @Before fun rememberSelection() { previousHero = RoutineRepository(context).selectedHeroId }
    @After fun restoreSelection() { RoutineRepository(context).selectedHeroId = previousHero }
    private fun launch() = ActivityScenario.launch<RoutineActivity>(
        Intent(context, RoutineActivity::class.java).putExtra(RoutineActivity.HERO_ID, "einstein")
    )

    @Test fun chosenHeroAndDateSurviveRecreation() {
        launch().use { scenario ->
            val date = RoutineDate(2024, 2, 29)
            onView(withId(R.id.iv_calendar)).perform(click())
            onView(isAssignableFrom(DatePicker::class.java)).perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> = isAssignableFrom(DatePicker::class.java)
                override fun getDescription() = "Select a leap day"
                override fun perform(controller: UiController, view: View) {
                    (view as DatePicker).updateDate(date.year, date.month - 1, date.day)
                    controller.loopMainThreadUntilIdle()
                }
            })
            onView(withId(android.R.id.button1)).perform(click())
            scenario.recreate()
            onView(withId(R.id.routine_title)).check(matches(withText(context.getString(R.string.routine_title, HeroCatalog.getOrDefault("einstein").name))))
            onView(withId(R.id.routine_date)).check(matches(withText(date.label("yyyy년 M월 d일 E요일"))))
        }
    }

    @Test fun rapidClicksOpenOneDetailWithTheClickedRoutine() {
        launch().use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            // Wait for RecyclerView layout through Espresso's UI-thread idling.
            onView(withId(R.id.rv_routine_list)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.rv_routine_list)
                val firstRow = requireNotNull(list.findViewHolderForAdapterPosition(0)).itemView
                firstRow.performClick()
                firstRow.performClick()
                val details = activity.supportFragmentManager.fragments.filterIsInstance<RoutineDetailBottomSheet>()
                assertEquals(1, details.size)
                assertEquals("기상", details.single().requireView().findViewById<TextView>(R.id.detail_title).text.toString())
                assertEquals("아인슈타인", details.single().requireView().findViewById<TextView>(R.id.detail_hero).text.toString())
            }
        }
    }

    @Test fun openFutureDetailBecomesAvailableAtItsDateBoundary() {
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val tomorrow = RoutineDate.today().plusDays(1)
                val detail = RoutineDetailBottomSheet.newInstance(
                    RoutineScope("einstein", tomorrow), RoutineCatalog.all.first(), "아인슈타인"
                )
                detail.showNow(activity.supportFragmentManager, "midnight_test")
                val complete = detail.requireView().findViewById<Button>(R.id.btn_start)
                assertFalse(complete.isEnabled)
                detail.refreshCompletionAvailability(tomorrow)
                assertTrue(complete.isEnabled)
            }
        }
    }
}
