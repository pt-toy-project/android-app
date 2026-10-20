package com.example.test

import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test.Hero.HeroCatalog
import com.example.test.Hero.HeroDayCatalog
import com.example.test.Hero.HeroQuestCatalog
import com.example.test.Routine.RoutineDate
import com.example.test.data.AppRepository
import com.example.test.data.CompletionOutcome
import com.example.test.data.ProgressSummary
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
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
class ProgressFlowTest {
    private val context get() = RuntimeEnvironment.getApplication()
    @Before fun returningUser() {
        context.getSharedPreferences(TutorialActivity.PREFS, Context.MODE_PRIVATE).edit().putBoolean("completed", true).commit()
    }
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
    @Test fun streakHandlesLeapDayGapsAndUnfinishedToday() {
        val today = RoutineDate(2024, 3, 1)
        val previous = setOf(RoutineDate(2024, 2, 28), RoutineDate(2024, 2, 29))
        assertEquals(2, ProgressSummary.streak(previous, today))
        assertEquals(3, ProgressSummary.streak(previous + today, today))
        assertEquals(1, ProgressSummary.streak(setOf(today, today.plusDays(-2)), today))
        assertEquals(0, ProgressSummary.streak(setOf(today.plusDays(-2)), today))
        assertEquals(0, ProgressSummary.streak(emptySet(), today))
    }
    @Test fun badgesBecomeEarnedOnlyAtTheirActualCompletionThreshold() {
        listOf(1, 10, 30, 100).forEach { threshold ->
            assertFalse(ProgressSummary(threshold - 1, 1, 0, 1).earned(threshold))
            assertTrue(ProgressSummary(threshold, 1, 0, 1).earned(threshold))
        }
    }
    @Test fun progressKeepsDeletedHistoryAndChecklistStreakWithoutDuplicateRewards() {
        var today = RoutineDate(2024, 2, 28)
        AppRepository(context, todayProvider = { today }).use { repo ->
            val hero = HeroCatalog.default
            val block = HeroDayCatalog.forHero(hero).blocks.first()
            val id = repo.startHeroDay(hero).first()
            val duplicate = repo.addRoutine(hero.id, block.id, "내 실천", "08:00")
            repo.complete(id, today)
            today = today.plusDays(1); repo.complete(id, today)
            assertEquals(CompletionOutcome.ALREADY_COMPLETED, repo.complete(duplicate, today))
            today = today.plusDays(1)
            assertTrue(repo.setQuestChecked(id, block.quests.first().id, today, true))
            val expected = ProgressSummary(2, 3, 20, 3)
            assertEquals(expected, repo.progressSummary())
            repo.deleteRoutine(id); repo.deleteRoutine(duplicate)
            assertEquals(expected, repo.progressSummary())
            assertEquals(ProgressSummary(1, 1, 10, 1), repo.progressSummary(today.plusDays(-2)))
            assertEquals(20L, repo.balance())
        }
    }
    @Test fun statisticsOpenRealMonthlyCalendarAndNeverGrantAnotherReward() {
        AppRepository(context).use { repo -> repo.complete(repo.startHeroDay(HeroCatalog.default).first(), RoutineDate.today()) }
        val controller = Robolectric.buildActivity(ProgressActivity::class.java).setup().visible()
        try {
            var a = controller.get(); drain(a)
            assertEquals(a.getString(R.string.progress_totals, 1, 1, 1, 10L), a.findViewById<TextView>(R.id.progress_totals).text.toString())
            val first = a.findViewById<View>(R.id.progress_badges).findViewWithTag<android.view.ViewGroup>("badge:1")
            val progress = (first.getChildAt(0) as android.view.ViewGroup).getChildAt(2) as LinearProgressIndicator
            assertEquals(1, progress.progress)
            controller.recreate(); a = controller.get(); drain(a)
            a.findViewById<View>(R.id.progress_month_open).performClick()
            val intent = shadowOf(a).nextStartedActivity
            assertEquals(MainActivity::class.java.name, intent.component!!.className)
            val home = Robolectric.buildActivity(MainActivity::class.java, intent).setup().visible()
            try { drain(home.get()); assertEquals(View.VISIBLE, home.get().findViewById<View>(R.id.calendar_month_summary).visibility) }
            finally { home.pause().stop().destroy() }
            AppRepository(context).use { assertEquals(10L, it.balance()); assertEquals(1, it.history().size) }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun goalRecommendationResetsConflictingSearchAndSurvivesRotation() {
        val controller = Robolectric.buildActivity(CharacterSelectActivity::class.java).setup().visible()
        try {
            var a = controller.get()
            a.findViewById<EditText>(R.id.heroSearch).setText("없는인물987")
            val goal = "글쓰기·창작"
            a.findViewById<ChipGroup>(R.id.hero_goal_suggestions).findViewWithTag<Chip>(goal).performClick()
            assertEquals("", a.findViewById<EditText>(R.id.heroSearch).text.toString())
            val expected = HeroCatalog.all.count { HeroQuestCatalog.goalFor(it.id) == goal }
            assertTrue(expected > 0)
            assertEquals(a.getString(R.string.hero_result_count, expected, HeroCatalog.all.size), a.findViewById<TextView>(R.id.heroResultCount).text.toString())
            controller.recreate(); a = controller.get()
            assertTrue(a.findViewById<ChipGroup>(R.id.hero_goal_suggestions).findViewWithTag<Chip>(goal).isChecked)
            assertEquals(goal, a.findViewById<TextView>(R.id.hero_goal).text.toString())
            assertEquals(a.getString(R.string.hero_result_count, expected, HeroCatalog.all.size), a.findViewById<TextView>(R.id.heroResultCount).text.toString())
        } finally { controller.pause().stop().destroy() }
    }
}
