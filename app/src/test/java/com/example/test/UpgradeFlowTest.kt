package com.example.test

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test.Hero.HeroCatalog
import com.example.test.data.AppRepository
import com.example.test.focus.FocusPhase
import com.example.test.focus.FocusTimerStore
import com.google.android.material.chip.ChipGroup
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class UpgradeFlowTest {
    private val context get() = RuntimeEnvironment.getApplication()
    @Before fun returningUser() {
        context.getSharedPreferences(TutorialActivity.PREFS, Context.MODE_PRIVATE).edit().putBoolean("completed", true).commit()
    }
    private fun drain(activity: AppCompatActivity) {
        repeat(3) {
            shadowOf(Looper.getMainLooper()).idle()
            var done = false
            AppIo.run(activity, { true }) { done = true }
            val deadline = System.nanoTime() + 5_000_000_000L
            while (!done && System.nanoTime() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }
            assertTrue("Background work timed out", done)
        }
    }

    @Test fun guestStartsCatalogInOneTapWithoutChangingExistingData() {
        context.getSharedPreferences(TutorialActivity.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        val id = AppRepository(context).use { it.addPersonalPlan("내 기록", "08:00") }
        val controller = Robolectric.buildActivity(TutorialActivity::class.java).setup().visible()
        try {
            val a = controller.get()
            assertEquals(View.GONE, a.findViewById<View>(R.id.intro_google).visibility)
            a.findViewById<View>(R.id.intro_next).performClick()
            assertEquals(CharacterSelectActivity::class.java.name, shadowOf(a).nextStartedActivity.component!!.className)
            assertFalse(TutorialActivity.needed(a))
            AppRepository(a).use { assertNotNull(it.getRoutine(id)); assertEquals(0L, it.balance()) }
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun emptySearchResetClearsAllFiltersAndSurvivesRotation() {
        val controller = Robolectric.buildActivity(CharacterSelectActivity::class.java).setup().visible()
        try {
            val a = controller.get()
            a.findViewById<ChipGroup>(R.id.hero_fields).getChildAt(HeroCatalog.fields.indexOf("과학")).performClick()
            a.findViewById<EditText>(R.id.heroSearch).setText("없는인물987")
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.hero_empty_actions).visibility)
            a.findViewById<View>(R.id.hero_reset_filters).performClick()
            assertEquals("", a.findViewById<EditText>(R.id.heroSearch).text.toString())
            assertEquals(a.getString(R.string.hero_result_count, 30, 30), a.findViewById<TextView>(R.id.heroResultCount).text.toString())
            controller.recreate()
            assertEquals(View.GONE, controller.get().findViewById<View>(R.id.hero_empty_actions).visibility)
            assertEquals(controller.get().getString(R.string.hero_result_count, 30, 30), controller.get().findViewById<TextView>(R.id.heroResultCount).text.toString())
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun timerResetFromHomeRetainsPracticeAfterRotationAndCanRestart() {
        val id = AppRepository(context).use { it.addPersonalPlan("읽기", "08:00") }
        val store = FocusTimerStore(context)
        store.start(id, "읽기", 5)
        // Home opens FocusActivity without a routine extra.
        val controller = Robolectric.buildActivity(FocusActivity::class.java).setup().visible()
        try {
            drain(controller.get())
            controller.get().findViewById<View>(R.id.focus_reset).performClick()
            (ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog).getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
            drain(controller.get())
            assertNull(store.read())
            controller.recreate()
            drain(controller.get())
            assertEquals("읽기", controller.get().findViewById<TextView>(R.id.focus_title).text.toString())
            assertTrue(controller.get().findViewById<View>(R.id.focus_primary).isEnabled)
            controller.get().findViewById<View>(R.id.focus_primary).performClick()
            assertEquals(id, store.read()!!.routineId)
            assertEquals(FocusPhase.RUNNING, store.read()!!.phase)
            AppRepository(context).use { assertEquals(0L, it.balance()) }
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun backgroundTimerExpiryRemainsReachableFromTodayWithoutReward() {
        val id = AppRepository(context).use { it.addPersonalPlan("읽기", "08:00") }
        val store = FocusTimerStore(context)
        val session = store.start(id, "읽기", 1)
        ShadowSystemClock.advanceBy(Duration.ofMinutes(1))
        assertNotNull(store.finish(session.token))
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a = controller.get(); drain(a)
            val action = a.findViewById<TextView>(R.id.timeline_focus_resume)
            assertEquals(View.VISIBLE, action.visibility)
            assertEquals(a.getString(R.string.upgrade_focus_finished, "읽기"), action.text.toString())
            action.performClick()
            assertEquals(FocusActivity::class.java.name, shadowOf(a).nextStartedActivity.component!!.className)
            AppRepository(context).use { assertEquals(0L, it.balance()); assertTrue(it.history().isEmpty()) }
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun timerBackgroundReturnShowsEndActionAndResetClearsOldAlert() {
        val id = AppRepository(context).use { it.addPersonalPlan("읽기", "08:00") }
        FocusTimerStore(context).start(id, "읽기", 1)
        val controller = Robolectric.buildActivity(FocusActivity::class.java).setup().visible()
        try {
            drain(controller.get())
            controller.pause().stop()
            ShadowSystemClock.advanceBy(Duration.ofMinutes(1))
            controller.start().resume().visible()
            drain(controller.get())
            assertEquals(FocusPhase.FINISHED, FocusTimerStore(context).read()!!.phase)
            assertEquals(controller.get().getString(R.string.polish_focus_reward_notice), controller.get().findViewById<TextView>(R.id.focus_alert).text.toString())
            controller.get().findViewById<View>(R.id.focus_reset).performClick()
            (ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog).getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
            drain(controller.get())
            assertEquals("", controller.get().findViewById<TextView>(R.id.focus_alert).text.toString())
            AppRepository(context).use { assertEquals(0L, it.balance()) }
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun disabledTimerNotificationsOfferWorkingSettingsRoute() {
        SettingsPreferences(context).alarmEnabled = false
        val id = AppRepository(context).use { it.addPersonalPlan("읽기", "08:00") }
        FocusTimerStore(context).start(id, "읽기", 5)
        val controller = Robolectric.buildActivity(FocusActivity::class.java).setup().visible()
        try {
            val a = controller.get(); drain(a)
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.focus_notification_settings).visibility)
            a.findViewById<View>(R.id.focus_notification_settings).performClick()
            assertEquals(SettingActivity::class.java.name, shadowOf(a).nextStartedActivity.component!!.className)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun previouslyDeniedNotificationsOpenSystemSettings() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        context.getSharedPreferences("notification_permission_ui", Context.MODE_PRIVATE).edit().putBoolean("requested", true).commit()
        val controller = Robolectric.buildActivity(SettingActivity::class.java).setup().visible()
        try {
            val a = controller.get()
            a.findViewById<View>(R.id.settings_notification_permission).performClick()
            val intent = shadowOf(a).nextStartedActivity
            assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
            assertEquals(a.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun localBackupPrecedesOptionalAccountAndLaunchesFilePicker() {
        val controller = Robolectric.buildActivity(BackupActivity::class.java).setup().visible()
        try {
            val a = controller.get()
            val root = a.window.decorView
            root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, 360, 800)
            val local = IntArray(2); val account = IntArray(2)
            a.findViewById<View>(R.id.backup_export).getLocationOnScreen(local)
            a.findViewById<View>(R.id.backup_account).getLocationOnScreen(account)
            assertTrue(local[1] < account[1])
            assertEquals(View.GONE, a.findViewById<View>(R.id.backup_sign_in).visibility)
            a.findViewById<View>(R.id.backup_export).performClick()
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, shadowOf(a).nextStartedActivity.action)
        } finally { controller.pause().stop().destroy() }
    }
}
