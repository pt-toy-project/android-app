package com.example.test.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.test.R
import com.example.test.Routine.RoutineDate
import com.example.test.RoutineActivity
import com.example.test.SettingsPreferences
import com.example.test.data.AppRepository
import com.example.test.data.CompletionOutcome
import com.example.test.data.UserRoutine
import java.util.TimeZone

/** Counts accepted scheduling requests; they do not promise that the OS will deliver an alert. */
data class AlarmSyncResult(
    val exactScheduled: Int = 0,
    val inexactScheduled: Int = 0,
    val skipped: Int = 0,
    val notificationsBlocked: Boolean = false,
    val exactPermissionMissing: Boolean = false,
    val errors: List<String> = emptyList()
)

/**
 * Blocking scheduling/database API: call from AppIo or another worker, never from the UI thread.
 * All instances share a process lock; receivers also use a serial executor. Each routine has one
 * ordinary pending alarm and at most one explicit snooze, both immutable and identified by Long ID.
 */
class AlarmCoordinator(context: Context) {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(AlarmManager::class.java)
    private val notifications = this.context.getSystemService(NotificationManager::class.java)
    private val store = AlarmStore(this.context)
    private val settings = SettingsPreferences(this.context)

    fun reconcile(): AlarmSyncResult = synchronized(LOCK) {
        createChannels()
        val blocked = !notificationsAllowed()
        val exactMissing = !canUseExactAlarms()
        return@synchronized try {
            AppRepository(context).use { repository ->
                reconcileLocked(repository, blocked, exactMissing)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Could not reconcile routine reminders", exception)
            AlarmSyncResult(notificationsBlocked = blocked, exactPermissionMissing = exactMissing,
                errors = listOf(context.getString(R.string.alarm_error_database)))
        }
    }

    fun notificationsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || notifications.getNotificationChannel(channelId())?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun canUseExactAlarms(): Boolean = Build.VERSION.SDK_INT < 31 || runCatching {
        manager.canScheduleExactAlarms()
    }.getOrDefault(false)

    /** Uses the same sound/channel choice as routine reminders; never writes completion or coins. */
    internal fun postUtility(title: String, text: String, open: PendingIntent, id: Int): Boolean = synchronized(LOCK) {
        createChannels()
        if (!settings.alarmEnabled || !notificationsAllowed()) return@synchronized false
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return@synchronized false
        val notification = NotificationCompat.Builder(context, channelId())
            .setSmallIcon(R.drawable.ic_alarm_notification).setContentTitle(title).setContentText(text)
            .setContentIntent(open).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (Build.VERSION.SDK_INT < 26 && settings.soundEnabled) notification.setDefaults(NotificationCompat.DEFAULT_SOUND)
        if (!settings.soundEnabled) notification.setSilent(true)
        NotificationManagerCompat.from(context).notify("experience", id, notification.build())
        true
    }

    fun cancel(routineId: Long) = synchronized(LOCK) {
        if (routineId <= 0) return@synchronized
        cancelAlarm(routineId, AlarmIdentity.REGULAR)
        cancelAlarm(routineId, AlarmIdentity.SNOOZE)
        notifications.cancel(notificationTag(routineId), NOTIFICATION_ID)
        // Cancellation stops delivery; it must not make a previously delivered date eligible again.
        store.remove(routineId, preserveDeliveryHistory = true)
    }

    fun cancelAll() = synchronized(LOCK) { store.trackedIds().forEach(::cancel) }

    internal fun fire(intent: Intent) = synchronized(LOCK) {
        if (intent.action != ACTION_FIRE) return@synchronized
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return@synchronized
        if (id <= 0 || kind !in listOf(AlarmIdentity.REGULAR, AlarmIdentity.SNOOZE)) return@synchronized
        val scheduled = store.read(id, kind) ?: return@synchronized
        if (scheduled.token != intent.getStringExtra(EXTRA_TOKEN)) return@synchronized
        val now = System.currentTimeMillis()
        val timeZone = TimeZone.getDefault()
        val today = RoutineDate.today(timeZone)
        createChannels()
        AppRepository(context).use { repository ->
            val routine = repository.getRoutine(id)
            val eligible = notificationsAllowed() && AlarmDeliveryPolicy.eligible(
                routine, scheduled, today, now, timeZone.id, settings.alarmEnabled,
                repository.isCompleted(id, scheduled.date), store.deliveredDate(id, scheduled.date)
            )
            // Consuming metadata before the next schedule makes repeated copies of this broadcast stale.
            store.forgetSchedule(id, kind)
            if (eligible && routine != null && store.claimDelivery(scheduled)) {
                try {
                    showNotification(routine, scheduled)
                } catch (exception: Exception) {
                    Log.e(TAG, "Routine notification could not be posted", exception)
                }
            }
            reconcileLocked(repository, !notificationsAllowed(), !canUseExactAlarms())
        }
    }

    internal fun act(intent: Intent) = synchronized(LOCK) {
        if (intent.action !in listOf(ACTION_COMPLETE, ACTION_SNOOZE)) return@synchronized
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: return@synchronized
        val date = RoutineDate.parse(intent.getStringExtra(EXTRA_DATE)) ?: return@synchronized
        if (id <= 0 || store.activeToken(id) != token || store.activeDate(id) != date) return@synchronized
        AppRepository(context).use { repository ->
            val routine = repository.getRoutine(id)
            // A notification action queued before an edit/timezone change must not revive its old alert.
            if (!AlarmDeliveryPolicy.actionMatchesSnapshot(routine, store.activeConfiguration(id),
                store.activeTimeZone(id), TimeZone.getDefault().id)) {
                dismissNotification(id)
                cancelAlarm(id, AlarmIdentity.SNOOZE)
                reconcileLocked(repository, !notificationsAllowed(), !canUseExactAlarms())
                return@synchronized
            }
            if (intent.action == ACTION_COMPLETE) {
                // Completion+coins are one database transaction and remain idempotent across button taps.
                val outcome = repository.complete(id, date, RoutineDate.today())
                if (outcome == CompletionOutcome.COMPLETED || outcome == CompletionOutcome.ALREADY_COMPLETED ||
                    outcome == CompletionOutcome.ROUTINE_UNAVAILABLE || outcome == CompletionOutcome.NOT_SCHEDULED) {
                    dismissNotification(id)
                    cancelAlarm(id, AlarmIdentity.SNOOZE)
                }
            } else if (routine != null && routine.enabled && routine.alarmEnabled && settings.alarmEnabled &&
                date == RoutineDate.today() && !repository.isCompleted(id, date) && notificationsAllowed()) {
                val now = System.currentTimeMillis()
                val snoozeAt = AlarmIdentity.snoozeAt(now) ?: return@synchronized
                val candidateDate = RoutineDate.fromCalendar(java.util.GregorianCalendar().apply { timeInMillis = snoozeAt })
                // A snooze belongs to its original day. Do not alert tomorrow for yesterday's task.
                if (candidateDate == date) {
                    val occurrence = occurrence(routine, AlarmIdentity.SNOOZE, NextOccurrence(date, snoozeAt), TimeZone.getDefault())
                    if (schedule(occurrence) != null) dismissNotification(id)
                } else dismissNotification(id)
            } else dismissNotification(id)
            reconcileLocked(repository, !notificationsAllowed(), !canUseExactAlarms())
        }
    }

    private fun reconcileLocked(
        repository: AppRepository,
        blocked: Boolean,
        exactMissing: Boolean
    ): AlarmSyncResult {
        val routines = repository.listRoutines()
        val presentIds = routines.map(UserRoutine::id).toSet()
        val errors = mutableListOf<String>()
        var exact = 0
        var inexact = 0
        var skipped = 0
        val now = System.currentTimeMillis()
        val zone = TimeZone.getDefault()
        val today = RoutineDate.today(zone)
        (store.trackedIds() - presentIds).forEach { id -> runCatching { cancel(id) }.onFailure {
            errors += context.getString(R.string.alarm_error_cancel)
        } }
        for (routine in routines) {
            try {
                if (!settings.alarmEnabled || blocked || !routine.enabled || !routine.alarmEnabled) {
                    cancelAlarm(routine.id, AlarmIdentity.REGULAR)
                    cancelAlarm(routine.id, AlarmIdentity.SNOOZE)
                    dismissNotification(routine.id)
                    skipped++
                    continue
                }
                // Clear a displayed alert and its snooze as soon as the app records completion.
                val activeDate = store.activeDate(routine.id)
                if (activeDate != null && (activeDate != today || repository.isCompleted(routine.id, activeDate) ||
                    store.activeConfiguration(routine.id) != AlarmDeliveryPolicy.configuration(routine) || store.activeTimeZone(routine.id) != zone.id)) {
                    dismissNotification(routine.id)
                    cancelAlarm(routine.id, AlarmIdentity.SNOOZE)
                }
                val snooze = store.read(routine.id, AlarmIdentity.SNOOZE)
                if (snooze != null) {
                    // An inexact snooze may already be due while Android is still delaying delivery.
                    val valid = AlarmDeliveryPolicy.snoozeStillValid(routine, snooze, today, zone.id,
                        repository.isCompleted(routine.id, snooze.date), store.deliveredDate(routine.id, snooze.date))
                    if (valid) {
                        when (schedule(snooze)) {
                            true -> exact++
                            false -> inexact++
                            null -> errors += context.getString(R.string.alarm_error_schedule)
                        }
                    } else cancelAlarm(routine.id, AlarmIdentity.SNOOZE)
                }
                val existing = store.read(routine.id, AlarmIdentity.REGULAR)
                // Opening the app must not replace a due-but-delayed inexact alarm with tomorrow's.
                val dueToday = existing?.takeIf {
                    it.date == today && it.triggerAtMillis <= now && it.timeZoneId == zone.id &&
                        it.configuration == AlarmDeliveryPolicy.configuration(routine) &&
                        store.deliveredDate(routine.id, today) != today && !repository.isCompleted(routine.id, today)
                }
                var next = dueToday?.let { NextOccurrence(it.date, it.triggerAtMillis) }
                    ?: NextOccurrenceCalculator.next(routine.time, routine.weekdayMask, now, zone)
                // A regular occurrence alerts at most once per local day, even after editing its time.
                if (next != null && (store.deliveredDate(routine.id, next.date) == next.date || repository.isCompleted(routine.id, next.date))) {
                    next = NextOccurrenceCalculator.next(routine.time, routine.weekdayMask, next.triggerAtMillis, zone)
                }
                if (next == null) {
                    cancelAlarm(routine.id, AlarmIdentity.REGULAR)
                    skipped++
                    errors += context.getString(R.string.alarm_error_schedule)
                    continue
                }
                when (schedule(occurrence(routine, AlarmIdentity.REGULAR, next, zone))) {
                    true -> exact++
                    false -> inexact++
                    null -> errors += context.getString(R.string.alarm_error_schedule)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Could not schedule routine ${routine.id}", exception)
                errors += context.getString(R.string.alarm_error_schedule)
            }
        }
        return AlarmSyncResult(exact, inexact, skipped, blocked, exactMissing || inexact > 0, errors.distinct())
    }

    private fun occurrence(routine: UserRoutine, kind: String, next: NextOccurrence, zone: TimeZone) = ScheduledOccurrence(
        routine.id, kind, next.date, next.triggerAtMillis, zone.id,
        AlarmDeliveryPolicy.configuration(routine), AlarmIdentity.token(routine.id, next.date, kind, next.triggerAtMillis)
    )

    /** true=exact, false=inexact, null=request failed. Permission races fall back honestly. */
    private fun schedule(occurrence: ScheduledOccurrence): Boolean? {
        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent(occurrence.routineId, occurrence.kind).apply {
            putExtra(EXTRA_TOKEN, occurrence.token)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        var exact = false
        return try {
            if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
                try {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, occurrence.triggerAtMillis, pendingIntent)
                    exact = true
                } catch (_: SecurityException) {
                    // The special permission may be revoked between the check and scheduling.
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, occurrence.triggerAtMillis, pendingIntent)
                }
            } else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, occurrence.triggerAtMillis, pendingIntent)
            if (!store.write(occurrence)) {
                manager.cancel(pendingIntent)
                pendingIntent.cancel()
                null
            } else exact
        } catch (exception: Exception) {
            Log.e(TAG, "AlarmManager rejected routine scheduling", exception)
            // The PendingIntent now contains the replacement token, so preserve no misleading old metadata.
            runCatching { manager.cancel(pendingIntent); pendingIntent.cancel(); store.forgetSchedule(occurrence.routineId, occurrence.kind) }
            null
        }
    }

    private fun alarmIntent(id: Long, kind: String) = Intent(context, AlarmReceiver::class.java).apply {
        action = ACTION_FIRE
        data = Uri.parse(AlarmIdentity.uri(kind, id))
        putExtra(EXTRA_ID, id)
        putExtra(EXTRA_KIND, kind)
    }

    private fun cancelAlarm(id: Long, kind: String) {
        PendingIntent.getBroadcast(context, 0, alarmIntent(id, kind), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            manager.cancel(it)
            it.cancel()
        }
        store.forgetSchedule(id, kind)
    }

    private fun dismissNotification(id: Long) {
        notifications.cancel(notificationTag(id), NOTIFICATION_ID)
        store.dismiss(id)
    }

    private fun showNotification(routine: UserRoutine, occurrence: ScheduledOccurrence) {
        // Keep the runtime permission check adjacent to notify for permission races and lint validation.
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        if (!notificationsAllowed()) return
        val open = Intent(context, if (routine.source == com.example.test.data.RoutineSource.HERO_DAY) com.example.test.MainActivity::class.java else RoutineActivity::class.java).apply {
            data = Uri.parse(AlarmIdentity.uri("open", routine.id))
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("hero_id", routine.heroId)
            putExtra(EXTRA_ID, routine.id)
            putExtra("selected_date", occurrence.date.iso)
        }
        val pendingOpen = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channelId())
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(context.getString(R.string.alarm_notification_title, routine.title))
            .setContentText(context.getString(R.string.alarm_notification_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.alarm_notification_text)))
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setPriority(if (settings.soundEnabled) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_alarm_notification, context.getString(R.string.alarm_action_complete),
                actionIntent(ACTION_COMPLETE, AlarmIdentity.COMPLETE, routine.id, occurrence))
            .addAction(R.drawable.ic_alarm_notification, context.getString(R.string.alarm_action_snooze),
                actionIntent(ACTION_SNOOZE, "snooze_action", routine.id, occurrence))
        if (Build.VERSION.SDK_INT < 26 && settings.soundEnabled) notification.setDefaults(NotificationCompat.DEFAULT_SOUND)
        else if (!settings.soundEnabled) notification.setSilent(true)
        NotificationManagerCompat.from(context).notify(notificationTag(routine.id), NOTIFICATION_ID, notification.build())
    }

    private fun actionIntent(actionName: String, kind: String, id: Long, occurrence: ScheduledOccurrence): PendingIntent =
        PendingIntent.getBroadcast(context, 0, Intent(context, AlarmActionReceiver::class.java).apply {
            action = actionName
            data = Uri.parse(AlarmIdentity.uri(kind, id))
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TOKEN, occurrence.token)
            putExtra(EXTRA_DATE, occurrence.date.iso)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        notifications.createNotificationChannel(NotificationChannel(CHANNEL_NORMAL,
            context.getString(R.string.alarm_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.alarm_channel_description)
            setSound(Settings.System.DEFAULT_NOTIFICATION_URI, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        })
        notifications.createNotificationChannel(NotificationChannel(CHANNEL_SILENT,
            context.getString(R.string.alarm_silent_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.alarm_silent_channel_description)
            setSound(null, null)
            enableVibration(false)
        })
    }

    private fun channelId() = if (settings.soundEnabled) CHANNEL_NORMAL else CHANNEL_SILENT
    private fun notificationTag(id: Long) = "routine:$id"

    companion object {
        private val LOCK = Any()
        private const val TAG = "RoutineAlarms"
        private const val NOTIFICATION_ID = 1
        const val CHANNEL_NORMAL = "routine_reminders"
        const val CHANNEL_SILENT = "routine_reminders_silent"
        internal const val ACTION_FIRE = "com.example.test.alarm.FIRE"
        internal const val ACTION_COMPLETE = "com.example.test.alarm.COMPLETE"
        internal const val ACTION_SNOOZE = "com.example.test.alarm.SNOOZE"
        const val EXTRA_ID = "user_routine_id"
        internal const val EXTRA_KIND = "alarm_kind"
        internal const val EXTRA_TOKEN = "occurrence_token"
        internal const val EXTRA_DATE = "occurrence_date"
    }
}
