package com.example.test

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Render actual Android Views to review artifacts; these images are never bundled into the app. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenRenderTest {
    @Test fun renderFocusTimerWithActualSessionAndExpandedText() {
        val context = RuntimeEnvironment.getApplication()
        val id = com.example.test.data.AppRepository(context).use { it.addPersonalPlan("읽고 생각 정리하기", "08:00") }
        com.example.test.focus.FocusTimerStore(context).start(id, "읽고 생각 정리하기", 10)
        val controller = Robolectric.buildActivity(FocusActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            capture(activity, "focus-running")
            assertEquals(activity.getString(R.string.polish_focus_running), activity.findViewById<android.widget.TextView>(R.id.focus_phase).text.toString())
            activity.findViewById<View>(R.id.focus_primary).performClick()
            assertEquals(com.example.test.focus.FocusPhase.PAUSED, com.example.test.focus.FocusTimerStore(context).read()!!.phase)
            RuntimeEnvironment.setFontScale(2f)
            controller.recreate()
            capture(controller.get(), "focus-large-font")
            assertEquals(controller.get().getString(R.string.polish_focus_paused), controller.get().findViewById<android.widget.TextView>(R.id.focus_phase).text.toString())
            assertTrue(controller.get().findViewById<View>(R.id.focus_primary).height >= 48)
            com.example.test.data.AppRepository(context).use { assertEquals(0L, it.balance()) }
        } finally { controller.pause().stop().destroy() }
    }
    @org.junit.Before fun returningUser() {
        RuntimeEnvironment.getApplication().getSharedPreferences(TutorialActivity.PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("completed", true).commit()
    }

    private fun capture(activity: AppCompatActivity, name: String, dialogRoot: View? = null, width: Int = 360, height: Int = 800) {
        // Drain both the database action and the UI-triggered reload it schedules.
        repeat(2) {
            shadowOf(Looper.getMainLooper()).idle()
            var idle = false
            AppIo.run(activity, { true }) { idle = true }
            val limit = System.nanoTime() + 5_000_000_000L
            while (!idle && System.nanoTime() < limit) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }
            assertTrue(idle)
        }
        val root = dialogRoot ?: activity.window.decorView
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height)
        shadowOf(Looper.getMainLooper()).idle()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val renderedColors = buildSet {
            for (x in 0 until bitmap.width step 12) for (y in 0 until bitmap.height step 12) add(bitmap.getPixel(x, y))
        }
        assertTrue("Rendered frame is empty", renderedColors.size >= 3)
        val directory = File("build/verification-screens").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        bitmap.recycle()
    }

    @Test fun renderTodayCatalogPersonalPlanAndHeroDay() {
        listOf(MainActivity::class.java to "today", CharacterSelectActivity::class.java to "heroes",
            RoutineActivity::class.java to "personal").forEach { (type, name) ->
            val controller = Robolectric.buildActivity(type).setup().visible()
            try { capture(controller.get(), name) } finally { controller.pause().stop().destroy() }
        }
        val intent = Intent(RuntimeEnvironment.getApplication(), HeroDetailActivity::class.java)
            .putExtra(HeroDetailActivity.EXTRA_HERO_ID, "franklin")
        val controller = Robolectric.buildActivity(HeroDetailActivity::class.java, intent).setup().visible()
        try { capture(controller.get(), "hero-day") } finally { controller.pause().stop().destroy() }
    }

    @Test
    @Config(qualifiers = "w393dp-h851dp-night-mdpi")
    fun renderTodayAndSettingsWithNewBottomNavigation() {
        listOf(MainActivity::class.java to "v3.13-today-night",
            SettingActivity::class.java to "v3.13-settings-night").forEach { (type, name) ->
            val controller = Robolectric.buildActivity(type).setup().visible()
            try {
                val activity = controller.get()
                capture(activity, name, width = 393, height = 851)
                val nav = activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
                assertEquals(5, nav.menu.size())
                assertNotNull(nav.menu.findItem(R.id.nav_settings))
            } finally { controller.pause().stop().destroy() }
        }
    }

    @Test fun renderTodayTimetableAsSeparateClassesOnNarrowScreen() {
        val context = RuntimeEnvironment.getApplication()
        val weekday = com.example.test.timetable.TimetableEntry.weekday(com.example.test.Routine.RoutineDate.today())
        val ids = com.example.test.data.TimetableStore(context).use { store ->
            listOf(
                com.example.test.timetable.TimetableEntry(title = "데이터통신", weekday = weekday, startMinute = 540, endMinute = 720, location = "융합교육관 501호"),
                com.example.test.timetable.TimetableEntry(title = "객체지향개발론", weekday = weekday, startMinute = 780, endMinute = 870),
                com.example.test.timetable.TimetableEntry(title = "컴퓨터그래픽스", weekday = weekday, startMinute = 900, endMinute = 960)
            ).map(store::save)
        }
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            capture(activity, "today-timetable-compact", width = 320, height = 750)
            val rows = activity.findViewById<android.widget.LinearLayout>(R.id.timeline_timetable_classes)
            assertEquals(2, rows.childCount)
            assertEquals("09:00–12:00", rows.getChildAt(0).findViewById<android.widget.TextView>(R.id.today_class_time).text.toString())
            assertEquals("데이터통신", rows.getChildAt(0).findViewById<android.widget.TextView>(R.id.today_class_title).text.toString())
            assertEquals("그 외 수업 1개", activity.findViewById<android.widget.TextView>(R.id.timeline_timetable_more).text.toString())
            RuntimeEnvironment.setFontScale(1.8f)
            controller.recreate()
            capture(controller.get(), "today-timetable-large-font", width = 320, height = 750)
            assertEquals(android.widget.LinearLayout.VERTICAL,
                controller.get().findViewById<android.widget.LinearLayout>(R.id.timeline_date_header).orientation)
        } finally {
            controller.pause().stop().destroy()
            com.example.test.data.TimetableStore(context).use { store -> ids.forEach(store::delete) }
        }
    }
    @Test fun portraitDisplayIsCircularWithoutModifyingOriginalFile() {
        val image = android.widget.ImageView(RuntimeEnvironment.getApplication())
        AvatarView.bind(image, com.example.test.Hero.HeroCatalog.default.imageResId)
        val drawable = image.drawable
        drawable.setBounds(0, 0, 64, 64)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        drawable.draw(Canvas(bitmap))
        assertEquals(0, android.graphics.Color.alpha(bitmap.getPixel(0, 0)))
        assertEquals(255, android.graphics.Color.alpha(bitmap.getPixel(32, 32)))
        bitmap.recycle()
    }

    @Test fun renderAdoptedDayAfterActualCompletion() {
        val context = RuntimeEnvironment.getApplication()
        com.example.test.data.AppRepository(context).use { it.startHeroDay(com.example.test.Hero.HeroCatalog.getOrDefault("franklin")) }
        val intent = Intent(context, MainActivity::class.java).putExtra(RoutineActivity.HERO_ID, "franklin")
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup().visible()
        try {
            val activity = controller.get()
            capture(activity, "today-before-complete")
            activity.findViewById<android.widget.Button>(R.id.timeline_next_complete).performClick()
            capture(activity, "today-active")
            assertEquals(activity.getString(R.string.redesign_progress, 1, 8), activity.findViewById<android.widget.TextView>(R.id.personal_summary).text.toString())
            assertEquals(activity.getString(R.string.personal_wallet, 10L), activity.findViewById<android.widget.TextView>(R.id.timeline_wallet).text.toString())
            com.example.test.data.AppRepository(context).use { repo ->
                assertEquals(10L, repo.balance())
                assertEquals(1, repo.history().size)
            }
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun renderExpandedFontWithoutNavigationLabelClipping() {
        RuntimeEnvironment.setFontScale(2f)
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            capture(activity, "today-large-font")
            val nav = activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
            assertTrue(nav.height >= 104)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun renderAdoptedConcreteChecklist() {
        val context = RuntimeEnvironment.getApplication()
        val hero = com.example.test.Hero.HeroCatalog.getOrDefault("ada_lovelace")
        val work = com.example.test.Hero.HeroDayCatalog.forHero(hero).blocks.single { it.kind == com.example.test.Hero.DayKind.WORK }
        val routine = com.example.test.data.AppRepository(context).use { repo ->
            repo.startHeroDay(hero)
            repo.listRoutines(hero.id, source = com.example.test.data.RoutineSource.HERO_DAY).single { it.templateId == work.id }
        }
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            val sheet = PersonalRoutineDetailSheet.newInstance(routine, com.example.test.Routine.RoutineDate.today(), false)
            sheet.showNow(activity.supportFragmentManager, "render_quests")
            capture(activity, "quest-checklist", sheet.dialog!!.window!!.decorView)
            assertEquals(3, sheet.requireView().findViewById<android.widget.LinearLayout>(R.id.quest_detail_container).childCount)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun renderWelcomeBackupAndGroupedSettings() {
        listOf(TutorialActivity::class.java to "welcome", BackupActivity::class.java to "backup",
            SettingActivity::class.java to "settings").forEach { (type, name) ->
            val controller = Robolectric.buildActivity(type).setup().visible()
            try { capture(controller.get(), name) } finally { controller.pause().stop().destroy() }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-night-mdpi")
    fun renderTwoClassLauncherWidget() {
        val controller = Robolectric.buildActivity(SettingActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            val widget = activity.layoutInflater.inflate(R.layout.widget_timetable, null)
            widget.findViewById<android.widget.TextView>(R.id.widget_first_time).text = "09:00–12:00"
            widget.findViewById<android.widget.TextView>(R.id.widget_first_title).text = "데이터통신"
            widget.findViewById<android.widget.TextView>(R.id.widget_first_location).text = "융합교육관 501호"
            widget.findViewById<android.widget.TextView>(R.id.widget_second_time).text = "15:00–18:00"
            widget.findViewById<android.widget.TextView>(R.id.widget_second_title).text = "클라우드데이터베이스"
            widget.findViewById<android.widget.TextView>(R.id.widget_second_location).text = "융합교육관 202호"
            widget.findViewById<android.widget.TextView>(R.id.widget_detail).text = activity.getString(R.string.timetable_widget_open)
            capture(activity, "widget-two-classes-night", widget, width = 300, height = 180)
            assertTrue("Launcher widget footer must fit", widget.findViewById<View>(R.id.widget_detail).bottom <= widget.height - widget.paddingBottom)
        } finally { controller.pause().stop().destroy() }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-night-mdpi")
    fun renderRecommendationAdPlaceholderBetweenFiltersAndCatalog() {
        val controller = Robolectric.buildActivity(CharacterSelectActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            capture(activity, "recommendations-before-ad")
            val scroll = activity.findViewById<androidx.core.widget.NestedScrollView>(R.id.hero_scroll)
            val slot = activity.findViewById<View>(R.id.hero_ad_slot)
            scroll.scrollTo(0, slot.top - 16)
            capture(activity, "recommendations-ad-slot-night")
            assertEquals(View.VISIBLE, slot.visibility)
            assertTrue("Ad placement should be visible between filters and catalog", slot.getGlobalVisibleRect(android.graphics.Rect()))
            assertFalse(slot.isClickable)
        } finally { controller.pause().stop().destroy() }
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-night-mdpi")
    fun renderTimetableOnSmallDarkScreenAndLargeText() {
        val context = RuntimeEnvironment.getApplication()
        com.example.test.data.TimetableStore(context).use { it.save(
            com.example.test.timetable.TimetableEntry(title = "컴퓨터 그래픽스", weekday = 2,
                startMinute = 840, endMinute = 960, location = "공학관 310호")
        ) }
        val controller = Robolectric.buildActivity(TimetableActivity::class.java).setup().visible()
        try {
            capture(controller.get(), "timetable-week-dark", width = 320, height = 568)
            assertTrue("Weekly grid must be visible when the timetable opens",
                controller.get().window.decorView.findViewWithTag<View>("timetable-grid").getGlobalVisibleRect(android.graphics.Rect()))
            scrollTimetableGridToTop(controller.get())
            capture(controller.get(), "timetable-grid-dark", width = 320, height = 568)
            RuntimeEnvironment.setFontScale(2f)
            controller.recreate()
            capture(controller.get(), "timetable-large-font-start", width = 320, height = 568)
            scrollTimetableGridToTop(controller.get())
            capture(controller.get(), "timetable-large-font-dark", width = 320, height = 568)
            assertTrue(controller.get().findViewById<View>(R.id.timetable_add).height >= 48)
            assertTrue(controller.get().window.decorView.findViewWithTag<View>("day-header:1").height >= 48)
        } finally { controller.pause().stop().destroy() }
    }

    @Test
    @Config(qualifiers = "w393dp-h851dp-night-mdpi")
    fun importedTimetableKeepsWeekdaysVisibleWhileHoursScroll() {
        val context = RuntimeEnvironment.getApplication()
        val rows = visualTimetableRows()
        com.example.test.data.TimetableStore(context).use { it.import(rows) }
        val controller = Robolectric.buildActivity(TimetableActivity::class.java).setup().visible()
        try {
            capture(controller.get(), "timetable-imported-start", width = 393, height = 851)
            scrollTimetableGridToTop(controller.get())
            capture(controller.get(), "timetable-imported-grid-night", width = 393, height = 851)
            val root = controller.get().window.decorView
            val header = root.findViewWithTag<View>("day-header:0")
            assertEquals("월", (header as android.widget.TextView).text.toString())
            val body = root.findViewWithTag<androidx.core.widget.NestedScrollView>("timetable-grid-body")
            val before = IntArray(2).also(header::getLocationOnScreen)
            body.scrollTo(0, 4 * 56)
            capture(controller.get(), "timetable-imported-scrolled-night", width = 393, height = 851)
            val after = IntArray(2).also(header::getLocationOnScreen)
            assertEquals(before[1], after[1])
            assertTrue(body.scrollY > 0)
            assertTrue(root.findViewWithTag<View>("day-header:4").width >= 55)
            controller.get().findViewById<View>(R.id.timetable_list_mode).performClick()
            capture(controller.get(), "timetable-weekdays-list-night", width = 393, height = 851)
            assertEquals("월요일", root.findViewWithTag<android.widget.TextView>("list-day:0").text.toString())
        } finally { controller.pause().stop().destroy() }
    }

    @Test
    @Config(qualifiers = "w393dp-h851dp-night-mdpi")
    fun importConfirmationPreviewsClassesBeforeSaving() {
        val rows = visualTimetableRows()
        val controller = Robolectric.buildActivity(TimetableActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            capture(activity, "timetable-before-preview", width = 393, height = 851)
            val method = TimetableActivity::class.java.getDeclaredMethod("previewImport", List::class.java,
                String::class.java, Int::class.javaPrimitiveType).apply { isAccessible = true }
            method.invoke(activity, rows, "https://everytime.kr/@SamplePublicLink", 1)
            val field = TimetableActivity::class.java.getDeclaredField("transient").apply { isAccessible = true }
            val dialog = field.get(activity) as androidx.appcompat.app.AlertDialog
            assertTrue(dialog.isShowing)
            assertNotNull(dialog.window!!.decorView.findViewWithTag<View>("timetable-grid"))
            capture(activity, "timetable-import-preview-night", dialog.window!!.decorView, 393, 851)
        } finally { controller.pause().stop().destroy() }
    }

    private fun scrollTimetableGridToTop(activity: TimetableActivity) {
        val outer = activity.findViewById<androidx.core.widget.NestedScrollView>(R.id.timetable_scroll)
        val grid = activity.window.decorView.findViewWithTag<View>("timetable-grid")
        val area = android.graphics.Rect().apply { grid.getDrawingRect(this) }
        outer.offsetDescendantRectToMyCoords(grid, area)
        outer.scrollTo(0, (area.top - 8).coerceAtLeast(0))
    }

    private fun visualTimetableRows(): List<com.example.test.timetable.TimetableEntry> {
        val titles = listOf("프로젝트와 디자인", "데이터 구조 심화", "융합 디자인 세미나", "모바일 앱 제작",
            "컴퓨터 그래픽스", "클라우드 서비스", "과제 연구", "딥러닝 실습", "소프트웨어 개발")
        val fixture = javaClass.classLoader!!.getResourceAsStream("everytime-share-anonymized.xml")!!
        return fixture.use(com.example.test.timetable.EverytimeShareImporter::parse).entries.map { entry ->
            val number = entry.title.substringAfterLast(' ').toInt() - 1
            entry.copy(title = titles[number], location = "제2공학관 501호 실습실")
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-night-mdpi")
    fun renderSmallNightScreensWithDoubleFontAndReadableTimer() {
        RuntimeEnvironment.setFontScale(2f)
        val context = RuntimeEnvironment.getApplication()
        val id = com.example.test.data.AppRepository(context).use { it.addPersonalPlan("집중해서 한 쪽 읽기", "08:00") }
        com.example.test.focus.FocusTimerStore(context).start(id, "집중해서 한 쪽 읽기", 25)
        listOf(TutorialActivity::class.java to "welcome-small-night",
            CharacterSelectActivity::class.java to "heroes-small-night",
            SettingActivity::class.java to "settings-small-night",
            BackupActivity::class.java to "backup-small-night",
            FocusActivity::class.java to "focus-small-night").forEach { (type, name) ->
            val controller = Robolectric.buildActivity(type).setup().visible()
            try {
                val activity = controller.get()
                capture(activity, name, width = 320, height = 568)
                if (activity is FocusActivity) {
                    val clock = activity.findViewById<android.widget.TextView>(R.id.focus_clock)
                    assertEquals(1, clock.lineCount)
                    assertTrue("Timer must fit available width", clock.layout.getLineWidth(0) <= clock.width - clock.paddingLeft - clock.paddingRight)
                }
                if (activity is CharacterSelectActivity) {
                    val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.heroRecyclerView)
                    assertTrue("Catalog must remain reachable below filters", list.height > 0)
                    assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.hero_ad_slot).visibility)
                }
                if (activity is SettingActivity) {
                    assertTrue(activity.findViewById<View>(R.id.settings_backup).height >= 48)
                    assertEquals(activity.getString(R.string.settings_guest_heading),
                        activity.findViewById<android.widget.TextView>(R.id.settings_guest_title).text.toString())
                }
            } finally { controller.pause().stop().destroy() }
        }
    }

    private fun seedCalendar(): com.example.test.Routine.RoutineDate {
        val selected = com.example.test.Routine.RoutineDate(2024, 2, 29)
        var today = com.example.test.Routine.RoutineDate(2024, 2, 1)
        com.example.test.data.AppRepository(RuntimeEnvironment.getApplication(), todayProvider = { today }).use { repo ->
            val hero = com.example.test.Hero.HeroCatalog.default
            val ids = repo.startHeroDay(hero)
            today = selected
            listOf(1, 3, 8, 12, 18, 22, 27, 29).forEach { day ->
                val date = com.example.test.Routine.RoutineDate(2024, 2, day)
                ids.take(day % 4 + 1).forEach { repo.complete(it, date, selected) }
                val quest = com.example.test.Hero.HeroDayCatalog.forHero(hero).blocks.first().quests.first()
                repo.setQuestChecked(ids.first(), quest.id, date, true)
            }
        }
        return selected
    }

    @Test fun renderMonthlyCalendarFromSavedRecordsAndEarnedBadges() {
        val selected = seedCalendar()
        val intent = Intent(RuntimeEnvironment.getApplication(), MainActivity::class.java)
            .putExtra(TimelineActivity.SELECTED_DATE, selected.iso).putExtra("calendar_expanded", true)
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup().visible()
        try {
            val a = controller.get()
            capture(a, "calendar-month", height = 1000)
            assertTrue("Month rows must keep a natural height", a.findViewById<View>(R.id.calendar_grid).height <= 6 * 80)
            assertEquals(a.getString(R.string.calendar_coins, 20L), a.findViewById<android.widget.TextView>(R.id.calendar_coins).text.toString())
            a.findViewById<View>(R.id.calendar_open_day).performClick()
            capture(a, "calendar-day")
            val action = a.findViewById<View>(R.id.timeline_next_open)
            val bounds = android.graphics.Rect()
            assertTrue("Daily primary action remains visible", action.getGlobalVisibleRect(bounds))
        } finally { controller.pause().stop().destroy() }
        val progress = Robolectric.buildActivity(ProgressActivity::class.java).setup().visible()
        try { capture(progress.get(), "progress-badges") } finally { progress.pause().stop().destroy() }
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-night-mdpi")
    fun renderSmallMonthlyCalendarWithDoubleFontAndAllSevenColumns() {
        val selected = seedCalendar()
        RuntimeEnvironment.setFontScale(2f)
        val intent = Intent(RuntimeEnvironment.getApplication(), MainActivity::class.java)
            .putExtra(TimelineActivity.SELECTED_DATE, selected.iso).putExtra("calendar_expanded", true)
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup().visible()
        try {
            val a = controller.get()
            capture(a, "calendar-small-night", width = 320, height = 568)
            val grid = a.findViewById<android.widget.LinearLayout>(R.id.calendar_grid)
            assertTrue("Month must not expand into unbounded vertical space", grid.height <= 6 * 100)
            for (i in 0 until grid.childCount) {
                val row = grid.getChildAt(i) as android.widget.LinearLayout
                assertEquals(7, row.childCount)
                for (j in 0 until row.childCount) {
                    val cell = row.getChildAt(j)
                    assertTrue("Day column must fit", cell.left >= 0 && cell.right <= grid.width)
                    if (cell.tag != null) assertTrue("Date tap target must have enough height", cell.height >= 48)
                }
            }
            a.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_routine_list).scrollBy(0, 700)
            capture(a, "calendar-detail-small-night", width = 320, height = 568)
            a.findViewById<View>(R.id.calendar_open_day).performClick()
            capture(a, "calendar-day-small-night", width = 320, height = 568)
            val bounds = android.graphics.Rect()
            assertTrue("Returning from a scrolled month must reveal day navigation",
                a.findViewById<View>(R.id.calendar_previous).getGlobalVisibleRect(bounds))
        } finally { controller.pause().stop().destroy() }
        val progress = Robolectric.buildActivity(ProgressActivity::class.java).setup().visible()
        try { capture(progress.get(), "progress-small-night", width = 320, height = 568) }
        finally { progress.pause().stop().destroy() }
    }

    @Test
    @Config(qualifiers = "w393dp-h851dp-night-mdpi")
    fun monthHeaderFillsPhoneWidthAtLargeFont() {
        RuntimeEnvironment.setFontScale(2f)
        val intent = Intent(RuntimeEnvironment.getApplication(), MainActivity::class.java)
            .putExtra("calendar_expanded", true)
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup().visible()
        try {
            val activity = controller.get()
            capture(activity, "calendar-wide-font-night", width = 393, height = 851)
            val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_routine_list)
            val calendar = activity.findViewById<com.example.test.calendar.ActivityCalendarView>(R.id.activity_calendar)
            assertEquals(list.width - list.paddingLeft - list.paddingRight, calendar.width)
            val month = calendar.findViewById<android.widget.TextView>(R.id.calendar_month_summary)
            assertTrue(month.width >= 280)
        } finally { controller.pause().stop().destroy() }
    }

}
