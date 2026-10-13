package com.example.test.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.concurrent.Executors

/** goAsync keeps the receiver alive while the small local database/scheduling operation runs. */
private object ReceiverWork {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "routine-alarm-receiver").apply { isDaemon = true }
    }

    fun run(result: BroadcastReceiver.PendingResult, action: () -> Unit) {
        try {
            executor.execute {
                try {
                    action()
                } catch (exception: Exception) {
                    Log.e("RoutineAlarms", "Reminder receiver could not finish its operation", exception)
                } finally {
                    result.finish()
                }
            }
        } catch (exception: Exception) {
            result.finish()
            Log.e("RoutineAlarms", "Reminder receiver worker was unavailable", exception)
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val incoming = intent ?: return
        if (incoming.action != AlarmCoordinator.ACTION_FIRE) return
        val snapshot = Intent(incoming)
        val appContext = context.applicationContext
        ReceiverWork.run(goAsync()) { AlarmCoordinator(appContext).fire(snapshot) }
    }
}

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val incoming = intent ?: return
        if (incoming.action !in setOf(AlarmCoordinator.ACTION_COMPLETE, AlarmCoordinator.ACTION_SNOOZE)) return
        val snapshot = Intent(incoming)
        val appContext = context.applicationContext
        ReceiverWork.run(goAsync()) { AlarmCoordinator(appContext).act(snapshot) }
    }
}

class AlarmRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val incoming = intent ?: return
        val exactPermissionChange = android.os.Build.VERSION.SDK_INT >= 31 &&
            incoming.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        if (incoming.action !in RESTORE_ACTIONS && !exactPermissionChange) return
        val appContext = context.applicationContext
        ReceiverWork.run(goAsync()) { AlarmCoordinator(appContext).reconcile() }
    }

    companion object {
        private val RESTORE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
