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
    @org.junit.Before fun returningUser() {
        RuntimeEnvironment.getApplication().getSharedPreferences(TutorialActivity.PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("completed", true).commit()
    }

    private fun capture(activity: AppCompatActivity, name: String, dialogRoot: View? = null) {
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
        root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 360, 800)
        shadowOf(Looper.getMainLooper()).idle()
        val bitmap = Bitmap.createBitmap(360, 800, Bitmap.Config.ARGB_8888)
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

}
