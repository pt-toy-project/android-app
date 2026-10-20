package com.example.test

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.example.test.data.AppRepository
import com.example.test.Hero.HeroCatalog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class TutorialAndBackupScreenTest {
    @Test fun firstLaunchShowsGuideWithoutAdoptingOrRequiringAnAccount() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            assertTrue(TutorialActivity.needed(activity))
            assertEquals(TutorialActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
            AppRepository(activity).use { assertTrue(it.listRoutines().isEmpty()); assertEquals(0L, it.balance()) }
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun guestCanFinishEveryPageAndGuideDoesNotRepeat() {
        val controller = Robolectric.buildActivity(TutorialActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            assertEquals(View.GONE, activity.findViewById<View>(R.id.intro_google).visibility)
            activity.findViewById<View>(R.id.intro_learn).performClick()
            repeat(3) { assertTrue(activity.findViewById<View>(R.id.intro_next).performClick()) }
            assertFalse(TutorialActivity.needed(activity))
            assertEquals(CharacterSelectActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
        } finally { controller.pause().stop().destroy() }
        val main = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try { assertNull(shadowOf(main.get()).nextStartedActivity) }
        finally { main.pause().stop().destroy() }
    }

    @Test fun notificationDetailIsNotDivertedIntoFirstRunGuide() {
        val context = RuntimeEnvironment.getApplication()
        val id = AppRepository(context).use { it.startHeroDay(HeroCatalog.default).first() }
        val intent = Intent(context, MainActivity::class.java).putExtra(RoutineActivity.USER_ROUTINE_ID, id)
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup().visible()
        try { assertNull(shadowOf(controller.get()).nextStartedActivity) }
        finally { controller.pause().stop().destroy() }
    }

    @Test fun replayAndSkipPreserveExistingPlansAndAccountSelection() {
        val context = RuntimeEnvironment.getApplication()
        val id = AppRepository(context).use { it.addPersonalPlan("나의 계획", "08:15") }
        context.getSharedPreferences("google_account", Context.MODE_PRIVATE).edit().putString("email", "test@example.invalid").commit()
        val controller = Robolectric.buildActivity(TutorialActivity::class.java,
            Intent(context, TutorialActivity::class.java).putExtra("replay", true)).setup().visible()
        try {
            val activity = controller.get()
            assertEquals(View.GONE, activity.findViewById<View>(R.id.intro_google).visibility)
            assertTrue(activity.findViewById<View>(R.id.intro_skip).performClick())
            AppRepository(context).use { assertEquals("나의 계획", it.getRoutine(id)!!.title) }
            assertEquals("test@example.invalid", GoogleAccountSession(context).email)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun settingsRoutesToWorkingBackupAndGuideScreens() {
        val controller = Robolectric.buildActivity(SettingActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            activity.findViewById<View>(R.id.settings_backup).performClick()
            assertEquals(BackupActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
            activity.findViewById<View>(R.id.settings_tutorial).performClick()
            val guide = shadowOf(activity).nextStartedActivity
            assertEquals(TutorialActivity::class.java.name, guide.component!!.className)
            assertTrue(guide.getBooleanExtra("replay", false))
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun documentBackupButtonsLaunchSystemPickersAndDoNotFakeGoogleSuccess() {
        val controller = Robolectric.buildActivity(BackupActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            activity.findViewById<View>(R.id.backup_export).performClick()
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, shadowOf(activity).nextStartedActivity.action)
            activity.findViewById<View>(R.id.backup_import).performClick()
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, shadowOf(activity).nextStartedActivity.action)
            activity.findViewById<View>(R.id.backup_google_upload).performClick()
            assertEquals(activity.getString(R.string.backup_sign_in_first), activity.findViewById<TextView>(R.id.backup_status).text.toString())
            assertNull(GoogleAccountSession(activity).email)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun expandedFontGuideKeepsNavigationAndWrapping() {
        RuntimeEnvironment.setFontScale(2f)
        val controller = Robolectric.buildActivity(TutorialActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            activity.window.decorView.apply {
                measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
                layout(0, 0, 360, 800)
            }
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(activity.findViewById<View>(R.id.intro_next).height > 0)
            assertEquals(Int.MAX_VALUE, activity.findViewById<TextView>(R.id.intro_title).maxLines)
            assertTrue(activity.findViewById<View>(R.id.intro_next).performClick())
        } finally { controller.pause().stop().destroy() }
    }
}
