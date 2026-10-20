package com.example.test.timetable

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.test.R
import com.example.test.TimetableActivity
import com.example.test.alarm.ReceiverWork
import com.example.test.data.TimetableStore
import java.util.Calendar

/** Only local timetable data appears on the launcher and in the optional notification. */
object TimetableSurfaces {
    const val ACTION_REFRESH = "com.example.test.TIMETABLE_REFRESH"
    private const val CHANNEL = "timetable_status"
    private const val NOTICE_ID = 3024
    private const val REQUEST_ID = 3025

    fun notificationsAllowed(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        return manager.areNotificationsEnabled() && (Build.VERSION.SDK_INT < 26 || manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)
    }

    fun refresh(context: Context) {
        val app = context.applicationContext
        val (entries, enabled) = TimetableStore(app).use { it.entries() to it.notificationEnabled() }
        val now = Calendar.getInstance()
        val day = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val agenda = TimetableAgenda.at(entries, day, minute)
        val today = agenda.today
        val current = agenda.current
        val next = agenda.next
        val title = when {
            current != null -> app.getString(R.string.timetable_widget_now, current.title)
            next != null -> app.getString(R.string.timetable_widget_next, next.timeLabel, next.title)
            today.isEmpty() -> app.getString(R.string.timetable_widget_no_class)
            else -> app.getString(R.string.timetable_widget_finished)
        }
        val detail = next?.location?.ifBlank { next.timeLabel } ?: app.getString(R.string.timetable_widget_open)
        val widget = AppWidgetManager.getInstance(app)
        val component = ComponentName(app, TimetableWidgetProvider::class.java)
        widget.getAppWidgetIds(component).forEach { id ->
            val views = RemoteViews(app.packageName, R.layout.widget_timetable)
            views.setTextViewText(R.id.widget_title, app.getString(R.string.timetable_widget_title))
            views.setTextViewText(R.id.widget_class, title)
            views.setTextViewText(R.id.widget_detail, detail)
            views.setOnClickPendingIntent(R.id.widget_root, openIntent(app))
            widget.updateAppWidget(id, views)
        }
        val manager = app.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, app.getString(R.string.timetable_notice_channel), NotificationManager.IMPORTANCE_LOW))
        }
        if (enabled && notificationsAllowed(app)) {
            manager.notify(NOTICE_ID, NotificationCompat.Builder(app, CHANNEL)
                .setSmallIcon(R.drawable.ic_calendar).setContentTitle(app.getString(R.string.timetable_widget_title))
                .setContentText(title).setStyle(NotificationCompat.BigTextStyle().bigText("$title · $detail"))
                .setContentIntent(openIntent(app)).setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW).build())
        } else manager.cancel(NOTICE_ID)
        if (enabled || widget.getAppWidgetIds(component).isNotEmpty()) schedule(app, now, today)
        else app.getSystemService(AlarmManager::class.java).cancel(refreshIntent(app))
    }

    private fun schedule(context: Context, now: Calendar, today: List<TimetableEntry>) {
        val currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val nextMinute = today.flatMap { listOf(it.startMinute, it.endMinute) }.filter { it > currentMinute }.minOrNull()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.SECOND, 2); set(Calendar.MILLISECOND, 0)
            if (nextMinute == null) { add(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }
            else { set(Calendar.HOUR_OF_DAY, nextMinute / 60); set(Calendar.MINUTE, nextMinute % 60) }
        }
        context.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC_WAKEUP, target.timeInMillis, refreshIntent(context))
    }

    private fun refreshIntent(context: Context) = PendingIntent.getBroadcast(context, REQUEST_ID,
        Intent(context, TimetableRefreshReceiver::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun openIntent(context: Context) = PendingIntent.getActivity(context, REQUEST_ID,
        Intent(context, TimetableActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class TimetableWidgetProvider : android.appwidget.AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ReceiverWork.run(goAsync()) { TimetableSurfaces.refresh(context) }
    }
}

class TimetableRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in setOf(TimetableSurfaces.ACTION_REFRESH, Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        ReceiverWork.run(goAsync()) { TimetableSurfaces.refresh(context) }
    }
}
