package com.example.test

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test.Hero.HeroCatalog
import com.example.test.Routine.RoutineDate
import com.example.test.data.AppRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class CalendarScreenTest {
    private val context get() = RuntimeEnvironment.getApplication()
    @Before fun returningUser() { context.getSharedPreferences(TutorialActivity.PREFS, Context.MODE_PRIVATE).edit().putBoolean("completed", true).commit() }
    private fun drain(activity: AppCompatActivity) {
        repeat(3) {
            shadowOf(Looper.getMainLooper()).idle()
            var ready = false
            AppIo.run(activity, { true }) { ready = true }
            val limit = System.nanoTime() + 5_000_000_000L
            while (!ready && System.nanoTime() < limit) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
            assertTrue(ready)
        }
    }
    @Test fun calendarButtonTogglesInlineMonthAndLeapDaySurvivesRotation() {
        val controller = Robolectric.buildActivity(MainActivity::class.java,
            Intent(context, MainActivity::class.java).putExtra(TimelineActivity.SELECTED_DATE, "2024-02-28")).setup().visible()
        try {
            var a = controller.get(); drain(a)
            assertEquals(View.GONE, a.findViewById<View>(R.id.calendar_month_summary).visibility)
            a.findViewById<View>(R.id.iv_calendar).performClick()
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.calendar_month_summary).visibility)
            a.findViewById<View>(R.id.activity_calendar).findViewWithTag<View>("calendar:2024-02-29").performClick()
            drain(a)
            controller.recreate(); a = controller.get(); drain(a)
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.calendar_month_summary).visibility)
            assertEquals(RoutineDate(2024, 2, 29).label("yyyy년 M월 d일 E요일"), a.findViewById<TextView>(R.id.routine_date).text.toString())
            assertTrue(a.findViewById<View>(R.id.activity_calendar).findViewWithTag<View>("calendar:2024-02-29").isSelected)
            a.findViewById<View>(R.id.calendar_open_day).performClick()
            assertEquals(View.GONE, a.findViewById<View>(R.id.calendar_month_summary).visibility)
            assertEquals(RoutineDate(2024, 2, 29).label("yyyy년 M월 d일 E요일"), a.findViewById<TextView>(R.id.routine_date).text.toString())
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun monthNavigationCrossesYearAndTodayButtonReturnsToActualToday() {
        val controller = Robolectric.buildActivity(MainActivity::class.java,
            Intent(context, MainActivity::class.java).putExtra(TimelineActivity.SELECTED_DATE, "2024-12-31")).setup().visible()
        try {
            val a = controller.get(); drain(a)
            a.findViewById<View>(R.id.iv_calendar).performClick()
            a.findViewById<View>(R.id.calendar_next).performClick(); drain(a)
            assertEquals(RoutineDate(2025, 1, 31).label("yyyy년 M월 d일 E요일"), a.findViewById<TextView>(R.id.routine_date).text.toString())
            a.findViewById<View>(R.id.calendar_today).performClick(); drain(a)
            assertEquals(RoutineDate.today().label("yyyy년 M월 d일 E요일"), a.findViewById<TextView>(R.id.routine_date).text.toString())
            assertEquals(View.GONE, a.findViewById<View>(R.id.calendar_today).visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun earnedCoinsComeFromRecordsAndRepeatedNavigationNeverPaysAgain() {
        val date = RoutineDate.today()
        AppRepository(context).use { repo -> val id = repo.startHeroDay(HeroCatalog.default).first(); repo.complete(id, date) }
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a = controller.get(); drain(a)
            repeat(2) {
                a.findViewById<View>(R.id.iv_calendar).performClick()
                a.findViewById<View>(R.id.calendar_previous).performClick(); drain(a)
                a.findViewById<View>(R.id.calendar_today).performClick(); drain(a)
            }
            assertEquals(a.getString(R.string.calendar_coins, 10L), a.findViewById<TextView>(R.id.calendar_coins).text.toString())
            AppRepository(context).use { assertEquals(10L, it.balance()); assertEquals(1, it.history().size) }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun futureDateShowsPlanButCannotCompleteOrReceiveCoins() {
        AppRepository(context).use { it.startHeroDay(HeroCatalog.default) }
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a = controller.get(); drain(a)
            a.findViewById<View>(R.id.calendar_next).performClick(); drain(a)
            assertEquals(a.getString(R.string.calendar_future), a.findViewById<TextView>(R.id.calendar_notice).text.toString())
            assertFalse(a.findViewById<View>(R.id.timeline_next_complete).isEnabled)
            AppRepository(context).use { assertEquals(0L, it.balance()); assertTrue(it.history().isEmpty()) }
        } finally { controller.pause().stop().destroy() }
    }
}
