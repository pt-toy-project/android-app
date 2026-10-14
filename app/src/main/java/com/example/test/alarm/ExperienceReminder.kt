package com.example.test.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import com.example.test.FocusActivity
import com.example.test.R
import com.example.test.SettingActivity
import com.example.test.SettingsPreferences
import com.example.test.focus.FocusTimerStore
import java.util.UUID

enum class ReminderResult { EXACT, APPROXIMATE, DISABLED, BLOCKED, FAILED }

/** Separate identities and tokens keep a preview/timer from consuming a routine's daily reminder. */
class ExperienceReminder(context: Context) {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(AlarmManager::class.java)
    private val prefs = this.context.getSharedPreferences("experience_reminders", Context.MODE_PRIVATE)

    fun preview(): ReminderResult = synchronized(LOCK) {
        val coordinator = AlarmCoordinator(context)
        coordinator.reconcile()
        if (!SettingsPreferences(context).alarmEnabled) return@synchronized ReminderResult.DISABLED
        if (!coordinator.notificationsAllowed()) return@synchronized ReminderResult.BLOCKED
        val token = UUID.randomUUID().toString()
        val deadline = SystemClock.elapsedRealtime() + 10_000
        if (!prefs.edit().putString("preview", token).putLong("preview_deadline", deadline).commit())
            return@synchronized ReminderResult.FAILED
        val result = schedule(PREVIEW, token, deadline)
        if (result == ReminderResult.FAILED) prefs.edit().clear().commit()
        result
    }

    fun scheduleFocus(): ReminderResult {
        val session = FocusTimerStore(context).read() ?: return ReminderResult.DISABLED
        if (session.phase != com.example.test.focus.FocusPhase.RUNNING) return ReminderResult.DISABLED
        val coordinator = AlarmCoordinator(context)
        coordinator.reconcile()
        if (!SettingsPreferences(context).alarmEnabled) return ReminderResult.DISABLED
        if (!coordinator.notificationsAllowed()) return ReminderResult.BLOCKED
        return schedule(FOCUS, session.token, SystemClock.elapsedRealtime() + session.remainingMillis)
    }

    fun cancelFocus() {
        PendingIntent.getBroadcast(context, 0, intent(FOCUS), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            manager.cancel(it); it.cancel()
        }
    }

    fun finishFocusIfDue() {
        val session = FocusTimerStore(context).read() ?: return
        deliver(intent(FOCUS).putExtra("token", session.token))
    }

    internal fun deliver(incoming: Intent) = synchronized(LOCK) {
        if (incoming.action != ACTION) return@synchronized
        val token = incoming.getStringExtra("token") ?: return@synchronized
        val kind = incoming.getStringExtra("kind")
        val coordinator = AlarmCoordinator(context)
        if (kind == PREVIEW) {
            if (prefs.getString("preview", null) != token || SystemClock.elapsedRealtime() < prefs.getLong("preview_deadline", Long.MAX_VALUE)) return@synchronized
            prefs.edit().clear().commit()
            val open = PendingIntent.getActivity(context, 2001, Intent(context, SettingActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            coordinator.postUtility(context.getString(R.string.polish_preview_title), context.getString(R.string.polish_preview_body), open, 2001)
        } else if (kind == FOCUS) {
            val session = FocusTimerStore(context).finish(token) ?: return@synchronized
            val open = PendingIntent.getActivity(context, 2002, Intent(context, FocusActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            coordinator.postUtility(context.getString(R.string.polish_focus_finished), session.title, open, 2002)
        }
    }

    private fun schedule(kind: String, token: String, deadline: Long): ReminderResult {
        val pending = PendingIntent.getBroadcast(context, 0, intent(kind).putExtra("token", token), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return try {
            if (AlarmCoordinator(context).canUseExactAlarms()) {
                try {
                    manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, pending)
                    return ReminderResult.EXACT
                } catch (_: SecurityException) { /* Revocation race: keep the timer with an approximate alert. */ }
            }
            manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, pending)
            ReminderResult.APPROXIMATE
        } catch (_: Exception) {
            runCatching { manager.cancel(pending); pending.cancel() }
            ReminderResult.FAILED
        }
    }

    private fun intent(kind: String) = Intent(context, ExperienceReminderReceiver::class.java).apply {
        action = ACTION; data = Uri.parse("heroday://experience/$kind"); putExtra("kind", kind)
    }

    companion object {
        private val LOCK = Any()
        internal const val ACTION = "com.example.test.alarm.EXPERIENCE"
        internal const val PREVIEW = "preview"
        internal const val FOCUS = "focus"
    }
}

class ExperienceReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val incoming = intent?.takeIf { it.action == ExperienceReminder.ACTION }?.let(::Intent) ?: return
        ReceiverWork.run(goAsync()) { ExperienceReminder(context.applicationContext).deliver(incoming) }
    }
}
