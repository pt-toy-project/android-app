package com.example.test.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import com.example.test.SettingsPreferences
import com.example.test.data.AppRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExperienceReminderTest {
    @Test fun previewUsesRealPendingIntentAndSameChannelWithoutGivingPoints() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = context.getSystemService(AlarmManager::class.java)
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val before = AppRepository(context).use { it.balance() }
        assertEquals(ReminderResult.EXACT, ExperienceReminder(context).preview())
        val alarm = requireNotNull(shadowOf(manager).nextScheduledAlarm)
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarm.type)
        val fire = shadowOf(alarm.operation).savedIntent
        ExperienceReminder(context).deliver(fire)
        assertEquals(0, context.getSystemService(NotificationManager::class.java).activeNotifications.size)
        org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofSeconds(10))
        ExperienceReminder(context).deliver(fire)
        val notifications = context.getSystemService(NotificationManager::class.java).activeNotifications
        assertEquals(1, notifications.size)
        assertEquals(AlarmCoordinator.CHANNEL_NORMAL, notifications.single().notification.channelId)
        ExperienceReminder(context).deliver(fire)
        assertEquals(1, context.getSystemService(NotificationManager::class.java).activeNotifications.size)
        AppRepository(context).use { assertEquals(before, it.balance()); assertTrue(it.listRoutines().isEmpty()) }
    }
    @Test fun disabledAndBlockedStatesDoNotCreateAPreviewAlarm() {
        val context = RuntimeEnvironment.getApplication()
        SettingsPreferences(context).alarmEnabled = false
        assertEquals(ReminderResult.DISABLED, ExperienceReminder(context).preview())
        SettingsPreferences(context).alarmEnabled = true
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(ReminderResult.BLOCKED, ExperienceReminder(context).preview())
        assertNull(shadowOf(context.getSystemService(AlarmManager::class.java)).nextScheduledAlarm)
    }
}
