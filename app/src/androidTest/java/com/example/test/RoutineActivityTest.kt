package com.example.test

import android.content.Intent
import android.os.SystemClock
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.test.Hero.HeroCatalog
import com.example.test.Routine.RoutineDate
import com.example.test.data.AppRepository
import com.example.test.data.UserRoutine
import org.hamcrest.Matcher
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

/** Uses unique test fixtures. Run on a dedicated test installation, never on a user's live app. */
@RunWith(AndroidJUnit4::class)
class RoutineActivityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var previousHero: String
    private lateinit var fixture: UserRoutine
    @Before fun setUp() {
        AppRepository(context).use { repo ->
            previousHero = repo.selectedHeroId
            val id = repo.addRoutine("einstein", "instrument_${UUID.randomUUID()}", "테스트 루틴", "06:00")
            fixture = requireNotNull(repo.getRoutine(id))
        }
    }
    @After fun cleanUp() {
        AppRepository(context).use { repo -> repo.deleteRoutine(fixture.id); repo.selectedHeroId = previousHero }
    }
    private fun launch() = ActivityScenario.launch<RoutineActivity>(Intent(context, RoutineActivity::class.java).putExtra(RoutineActivity.HERO_ID, "einstein"))

    private fun awaitUi(scenario: ActivityScenario<RoutineActivity>, condition: (RoutineActivity) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        do {
            var ready = false
            scenario.onActivity { ready = condition(it) }
            if (ready) return
            SystemClock.sleep(20)
        } while (SystemClock.uptimeMillis() < deadline)
        fail("UI did not reach the expected database-backed state")
    }

    @Test fun chosenHeroAndDateSurviveRecreation() {
        launch().use { scenario ->
            val date = RoutineDate(2024, 2, 29)
            onView(withId(R.id.iv_calendar)).perform(click())
            onView(isAssignableFrom(DatePicker::class.java)).perform(object : ViewAction {
                override fun getConstraints(): Matcher<android.view.View> = isAssignableFrom(DatePicker::class.java)
                override fun getDescription() = "Select leap day"
                override fun perform(controller: UiController, view: android.view.View) {
                    (view as DatePicker).updateDate(date.year, date.month - 1, date.day)
                    controller.loopMainThreadUntilIdle()
                }
            })
            onView(withId(android.R.id.button1)).perform(click())
            scenario.recreate()
            awaitUi(scenario) { it.findViewById<TextView>(R.id.routine_date).text.toString() == date.label("yyyy년 M월 d일 E요일") }
            scenario.onActivity { assertEquals(context.getString(R.string.routine_title, HeroCatalog.getOrDefault("einstein").name), it.findViewById<TextView>(R.id.routine_title).text.toString()) }
        }
    }

    @Test fun rapidClicksOpenOneDetailWithTheClickedRoutine() {
        launch().use { scenario ->
            awaitUi(scenario) { it.findViewById<RecyclerView>(R.id.rv_routine_list).findViewHolderForAdapterPosition(0) != null }
            scenario.onActivity { activity ->
                val row = requireNotNull(activity.findViewById<RecyclerView>(R.id.rv_routine_list).findViewHolderForAdapterPosition(0)).itemView
                row.performClick(); row.performClick()
            }
            awaitUi(scenario) { it.supportFragmentManager.fragments.filterIsInstance<PersonalRoutineDetailSheet>().isNotEmpty() }
            scenario.onActivity { activity ->
                val sheets = activity.supportFragmentManager.fragments.filterIsInstance<PersonalRoutineDetailSheet>()
                assertEquals(1, sheets.size)
                assertEquals("아인슈타인", sheets.single().requireView().findViewById<TextView>(R.id.personal_detail_hero).text.toString())
                assertFalse(sheets.single().requireView().findViewById<TextView>(R.id.personal_detail_title).text.isBlank())
            }
        }
    }

    @Test fun openFutureDetailBecomesAvailableWhenItsDateArrives() {
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val tomorrow = RoutineDate.today().plusDays(1)
                val sheet = PersonalRoutineDetailSheet.newInstance(fixture, tomorrow, false)
                sheet.showNow(activity.supportFragmentManager, "midnight_test")
                val button = sheet.requireView().findViewById<Button>(R.id.personal_detail_complete)
                assertFalse(button.isEnabled)
                sheet.refreshDateBoundary(tomorrow)
                assertTrue(button.isEnabled)
            }
        }
    }
}
