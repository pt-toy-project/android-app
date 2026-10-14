package com.example.test.alarm

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.test.MainActivity
import com.example.test.RoutineActivity
import com.example.test.SettingsPreferences
import com.example.test.TutorialActivity
import com.example.test.data.AppRepository
import com.example.test.data.BackupStore
import com.example.test.data.RoutineSource
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** These tests wait for Android AlarmManager broadcasts; they never invoke receiver methods directly. */
@RunWith(AndroidJUnit4::class)
class AlarmRuntimeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val notifications get() = context.getSystemService(NotificationManager::class.java)
    private var original: ByteArray? = null
    private val ids = mutableListOf<Long>()

    @Before fun before() {
        original = BackupStore(context).export()
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        SettingsPreferences(context).apply { alarmEnabled = true; soundEnabled = true }
        context.getSharedPreferences(TutorialActivity.PREFS, Context.MODE_PRIVATE).edit().putBoolean("completed", true).commit()
        notifications.cancelAll()
        assertTrue("Enable exact alarms on this test device before running", AlarmCoordinator(context).canUseExactAlarms())
    }
    @After fun after() {
        ids.forEach { AlarmCoordinator(context).cancel(it) }
        ExperienceReminder(context).cancelFocus()
        com.example.test.focus.FocusTimerStore(context).clear()
        original?.let { BackupStore(context).restore(it) }
        AlarmCoordinator(context).reconcile()
        notifications.cancelAll()
    }

    private fun await(timeoutMillis: Long, condition: () -> Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + timeoutMillis
        while (!condition() && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
        assertTrue("Android did not deliver the expected result in $timeoutMillis ms", condition())
    }

    @Test fun tenSecondPreviewIsDeliveredAfterSettingsClosesWithoutCompletingAnyRoutine() {
        val before = AppRepository(context).use { it.balance() }
        ActivityScenario.launch(com.example.test.SettingActivity::class.java).use { scenario ->
            assertEquals(ReminderResult.EXACT, ExperienceReminder(context).preview())
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            await(30_000) { notifications.activeNotifications.any { it.tag == "experience" && it.id == 2001 } }
            val notification = notifications.activeNotifications.single { it.tag == "experience" && it.id == 2001 }.notification
            assertEquals(AlarmCoordinator.CHANNEL_NORMAL, notification.channelId)
            assertNotNull(notifications.getNotificationChannel(notification.channelId).sound)
            AppRepository(context).use { assertEquals(before, it.balance()) }
        }
    }

    @Test fun scheduledRoutineNotificationsOpenTheirActualSourceAndCompleteOnlyOnce() {
        val block = "runtime_" + UUID.randomUUID().toString().replace("-", "")
        val due = (System.currentTimeMillis() / 60_000 + 1) * 60_000
        val time = SimpleDateFormat("HH:mm", Locale.ROOT).format(Date(due))
        val before = AppRepository(context).use { repo ->
            for (source in listOf(RoutineSource.HERO_DAY, RoutineSource.PERSONAL)) {
                val id = repo.addRoutine("sejong", block, "실행 알림 확인", time, source = source)
                repo.updateRoutine(repo.getRoutine(id)!!.copy(alarmEnabled = true))
                ids.add(id)
            }
            repo.balance()
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            val scheduled = AlarmCoordinator(context).reconcile()
            assertTrue(scheduled.errors.isEmpty())
            await(75_000) { ids.all { id -> notifications.activeNotifications.any { it.tag == "routine:$id" } } }
            val posted = ids.map { id -> notifications.activeNotifications.single { it.tag == "routine:$id" }.notification }
            for ((index, notification) in posted.withIndex()) {
                notification.contentIntent.send()
                await(10_000) {
                    var opened = false
                    instrumentation.runOnMainSync {
                        val expected = if (index == 0) MainActivity::class.java else RoutineActivity::class.java
                        opened = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                            .any { activity -> expected.isInstance(activity) && activity.intent.getLongExtra(AlarmCoordinator.EXTRA_ID, -1) == ids[index] }
                    }
                    opened
                }
            }
            // Both stale action copies and the second source must respect the canonical daily reward.
            repeat(2) { posted.forEach { notification -> notification.actions.first().actionIntent.send() } }
            await(10_000) { AppRepository(context).use { it.balance() == before + 10 } }
            Thread.sleep(500)
            AppRepository(context).use { repo ->
                assertEquals(before + 10, repo.balance())
                ids.forEach { assertTrue(repo.isCompleted(it, com.example.test.Routine.RoutineDate.today())) }
            }
        }
    }
}
