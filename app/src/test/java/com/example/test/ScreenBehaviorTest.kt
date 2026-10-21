package com.example.test

import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.example.test.Hero.HeroCatalog
import com.example.test.Hero.HeroDayCatalog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

/** Execute Android layout inflation and click handlers on the JVM; no real-device claims. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class ScreenBehaviorTest {
    @org.junit.Before fun returningUser() {
        RuntimeEnvironment.getApplication().getSharedPreferences(TutorialActivity.PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("completed", true).commit()
    }

    private fun render(activity: AppCompatActivity) {
        val root = activity.window.decorView
        val density = activity.resources.displayMetrics.density
        root.measure(View.MeasureSpec.makeMeasureSpec((360 * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((800 * density).toInt(), View.MeasureSpec.EXACTLY))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun drainWork(activity: AppCompatActivity) {
        shadowOf(Looper.getMainLooper()).idle()
        var finished = false
        AppIo.run(activity, { true }) { finished = true }
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!finished && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("UI/database work did not finish", finished)
        render(activity)
    }

    @Test fun planTabBecomesSettingsWhilePersonalPlansStayReachable() {
        val context = RuntimeEnvironment.getApplication()
        val planId = com.example.test.data.AppRepository(context).use { it.addPersonalPlan("남겨 둘 계획", "08:15") }
        val home = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = home.get()
            val nav = activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
            assertEquals(listOf(R.id.nav_home, R.id.nav_hero, R.id.nav_timetable, R.id.nav_myroom, R.id.nav_settings),
                (0 until nav.menu.size()).map { nav.menu.getItem(it).itemId })
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.personal_plans_open).visibility)
            activity.findViewById<View>(R.id.personal_plans_open).performClick()
            assertEquals(RoutineActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
            com.example.test.data.AppRepository(context).use { assertEquals("남겨 둘 계획", it.getRoutine(planId)?.title) }
            nav.selectedItemId = R.id.nav_settings
            assertEquals(SettingActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
        } finally { home.pause().stop().destroy() }
        val settings = Robolectric.buildActivity(SettingActivity::class.java).setup().visible()
        try {
            assertEquals(R.id.nav_settings,
                settings.get().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView).selectedItemId)
        } finally { settings.pause().stop().destroy() }
    }

    @Test fun timetableShowsSavedPersonalPlansAndOffersAddAndManage() {
        val context = RuntimeEnvironment.getApplication()
        val id = com.example.test.data.AppRepository(context).use {
            it.addPersonalPlan("운동하기", "18:30", weekdayMask = 5)
        }
        val controller = Robolectric.buildActivity(TimetableActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            drainWork(activity)
            val row = activity.window.decorView.findViewWithTag<TextView>("personal-plan:$id")
            assertTrue(row.text.toString().contains("운동하기"))
            assertTrue(row.text.toString().contains("월·수"))
            row.performClick()
            val detail = shadowOf(activity).nextStartedActivity
            assertEquals(RoutineActivity::class.java.name, detail.component!!.className)
            assertEquals(id, detail.getLongExtra(TimelineActivity.USER_ROUTINE_ID, -1))
            activity.findViewById<View>(R.id.timetable_personal_add).performClick()
            val add = shadowOf(activity).nextStartedActivity
            assertEquals(RoutineActivity::class.java.name, add.component!!.className)
            assertTrue(add.getBooleanExtra(RoutineActivity.OPEN_CREATE, false))
            val createScreen = Robolectric.buildActivity(RoutineActivity::class.java, add).setup().visible()
            try {
                shadowOf(Looper.getMainLooper()).idle()
                assertNotNull(createScreen.get().supportFragmentManager.findFragmentByTag("routine_editor"))
            } finally { createScreen.pause().stop().destroy() }
            activity.findViewById<View>(R.id.timetable_personal_open).performClick()
            assertEquals(RoutineActivity::class.java.name,
                shadowOf(activity).nextStartedActivity.component!!.className)
            com.example.test.data.AppRepository(context).use { assertEquals("운동하기", it.getRoutine(id)?.title) }
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun invalidTimetableShareShowsErrorWithoutScrollingToPageBottom() {
        val context = RuntimeEnvironment.getApplication()
        val intent = Intent(context, TimetableActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "잘못된 주소")
        }
        val controller = Robolectric.buildActivity(TimetableActivity::class.java, intent).setup().visible()
        try {
            val activity = controller.get()
            render(activity)
            val expected = activity.getString(R.string.timetable_link_invalid)
            assertEquals(expected, activity.findViewById<TextView>(R.id.timetable_status).text.toString())
            assertEquals(expected, activity.window.decorView.findViewById<TextView>(
                com.google.android.material.R.id.snackbar_text).text.toString())
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun todayEmptyStateActuallyOpensHeroCatalog() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            drainWork(activity)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.timeline_empty_card).visibility)
            assertTrue(activity.findViewById<Button>(R.id.empty_start).performClick())
            assertEquals(CharacterSelectActivity::class.java.name,
                shadowOf(activity).nextStartedActivity.component!!.className)
            assertNotNull(activity.findViewById<View>(R.id.timeline_progress))
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun catalogSearchShowsEmptyStateAndFieldsAreInteractive() {
        val controller = Robolectric.buildActivity(CharacterSelectActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            render(activity)
            val fields = activity.findViewById<com.google.android.material.chip.ChipGroup>(R.id.hero_fields)
            assertEquals(HeroCatalog.fields.size, fields.childCount)
            assertEquals(30, activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.heroRecyclerView).adapter!!.itemCount)
            assertEquals("인물 · 30명", activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.screen_toolbar).title.toString())
            assertEquals(View.GONE, activity.findViewById<View>(R.id.hero_browser_subtitle).visibility)
            activity.findViewById<EditText>(R.id.heroSearch).setText("no-catalog-match-392871")
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.heroEmptyState).visibility)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.heroRecyclerView).visibility)
            activity.findViewById<EditText>(R.id.heroSearch).setText("")
            fields.getChildAt(HeroCatalog.fields.indexOf("과학")).performClick()
            assertTrue(activity.findViewById<TextView>(R.id.heroResultCount).text.contains(
                HeroCatalog.filterByField("과학").size.toString()))
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun heroPreviewInflatesEightBlocksAndEvidenceSheetIsConnected() {
        val hero = HeroCatalog.getOrDefault("franklin")
        val intent = Intent(RuntimeEnvironment.getApplication(), HeroDetailActivity::class.java)
            .putExtra(HeroDetailActivity.EXTRA_HERO_ID, hero.id)
        val controller = Robolectric.buildActivity(HeroDetailActivity::class.java, intent).setup().visible()
        try {
            val activity = controller.get()
            render(activity)
            val blocks = activity.findViewById<LinearLayout>(R.id.historicalRoutineContainer)
            assertEquals(8, blocks.childCount)
            assertEquals(View.GONE, blocks.getChildAt(0).findViewById<View>(R.id.day_preview_description).visibility)
            assertEquals(HeroDayCatalog.NOTICE, activity.findViewById<TextView>(R.id.heroDetailBio).text.toString())
            assertEquals(hero.portraitCredit, activity.findViewById<TextView>(R.id.heroPortraitCredit).text.toString())
            blocks.getChildAt(0).findViewById<Button>(R.id.day_preview_details).performClick()
            activity.supportFragmentManager.executePendingTransactions()
            val sheet = activity.supportFragmentManager.findFragmentByTag("hero_evidence")!!
            assertNotNull(sheet.view!!.findViewById<View>(R.id.evidence_source))
            assertEquals("현대 적용", sheet.view!!.findViewById<TextView>(R.id.evidence_grade).text.toString())
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun personalPlanOpensEditorAndRejectsBlankTitle() {
        val controller = Robolectric.buildActivity(RoutineActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            drainWork(activity)
            assertTrue(activity.findViewById<Button>(R.id.empty_start).performClick())
            activity.supportFragmentManager.executePendingTransactions()
            val sheet = activity.supportFragmentManager.findFragmentByTag("routine_editor")!!
            sheet.view!!.findViewById<Button>(R.id.editor_save).performClick()
            assertNotNull(sheet.view!!.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.editor_title_layout).error)
            assertFalse(sheet.isRemoving)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun expandedFontRemovesPreviewLineLimit() {
        RuntimeEnvironment.setFontScale(2f)
        val intent = Intent(RuntimeEnvironment.getApplication(), HeroDetailActivity::class.java)
            .putExtra(HeroDetailActivity.EXTRA_HERO_ID, "nightingale")
        val controller = Robolectric.buildActivity(HeroDetailActivity::class.java, intent).setup().visible()
        try {
            val activity = controller.get()
            render(activity)
            val blocks = activity.findViewById<LinearLayout>(R.id.historicalRoutineContainer)
            assertEquals(Int.MAX_VALUE, blocks.getChildAt(0).findViewById<TextView>(R.id.day_preview_title).maxLines)
            assertEquals(Int.MAX_VALUE, blocks.getChildAt(0).findViewById<TextView>(R.id.day_preview_description).maxLines)
            assertTrue(activity.findViewById<Button>(R.id.heroPersonalRoutines).height > 0)
        } finally { controller.pause().stop().destroy() }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-night")
    fun nightThemeInflatesAndTextHasReadableContrast() {
        val controller = Robolectric.buildActivity(CharacterSelectActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            render(activity)
            assertTrue(ColorUtils.calculateContrast(activity.getColor(R.color.ui_text), activity.getColor(R.color.ui_surface)) >= 4.5)
            assertTrue(ColorUtils.calculateContrast(activity.getColor(R.color.ui_primary), activity.getColor(R.color.ui_surface)) >= 4.5)
            assertNotNull(activity.findViewById<View>(R.id.heroSearch))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun validPersonalEditorSaveCreatesOnlyPersonalData() {
        val controller = Robolectric.buildActivity(RoutineActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            drainWork(activity)
            activity.findViewById<Button>(R.id.empty_start).performClick()
            activity.supportFragmentManager.executePendingTransactions()
            val sheet = activity.supportFragmentManager.findFragmentByTag("routine_editor")!!
            sheet.view!!.findViewById<EditText>(R.id.editor_title).setText("나의 읽기 계획")
            sheet.view!!.findViewById<Button>(R.id.editor_save).performClick()
            drainWork(activity)
            com.example.test.data.AppRepository(activity).use { repo ->
                val plans = repo.listRoutines(source = com.example.test.data.RoutineSource.PERSONAL)
                assertEquals(1, plans.size)
                assertEquals("나의 읽기 계획", plans.single().title)
                assertEquals("personal", plans.single().heroId)
                assertTrue(repo.listRoutines(source = com.example.test.data.RoutineSource.HERO_DAY).isEmpty())
            }
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun wholeDayStartButtonActuallyAdoptsAndRoutesToToday() {
        val intent = Intent(RuntimeEnvironment.getApplication(), HeroDetailActivity::class.java)
            .putExtra(HeroDetailActivity.EXTRA_HERO_ID, "franklin")
        val controller = Robolectric.buildActivity(HeroDetailActivity::class.java, intent).setup().visible()
        try {
            val activity = controller.get()
            drainWork(activity)
            activity.findViewById<Button>(R.id.heroPersonalRoutines).performClick()
            drainWork(activity)
            com.example.test.data.AppRepository(activity).use { repo ->
                assertEquals(8, repo.listRoutines(source = com.example.test.data.RoutineSource.HERO_DAY).size)
                assertTrue(repo.listRoutines(source = com.example.test.data.RoutineSource.PERSONAL).isEmpty())
            }
            val destination = shadowOf(activity).nextStartedActivity
            assertEquals(MainActivity::class.java.name, destination.component!!.className)
            assertEquals("franklin", destination.getStringExtra(RoutineActivity.HERO_ID))
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun creditsRemainReachableFromToolbar() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            drainWork(activity)
            activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.screen_toolbar)
                .menu.performIdentifierAction(R.id.screen_credits, 0)
            assertEquals(CreditsActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun shopSettingsHistoryAndOfflineCreditsInflateWithCommonRoutes() {
        listOf(ShopActivity::class.java, SettingActivity::class.java, HistoryActivity::class.java, CreditsActivity::class.java).forEach { type ->
            val controller = Robolectric.buildActivity(type).setup().visible()
            try {
                val activity = controller.get()
                render(activity)
                assertNotNull(activity.findViewById<View>(R.id.screen_toolbar))
                if (activity is CreditsActivity) {
                    val web = activity.findViewById<android.webkit.WebView>(R.id.credits_webview)
                    assertFalse(web.settings.javaScriptEnabled)
                    assertTrue(web.settings.blockNetworkLoads)
                }
            } finally { controller.pause().stop().destroy() }
        }
    }

}
