package com.example.test.alarm

import android.content.Context
import com.example.test.Routine.RoutineDate

/** Small, non-secret scheduling metadata. Every access is protected by AlarmCoordinator's lock. */
internal class AlarmStore(context: Context, fileName: String = "routine_alarm_state") {
    private val preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    fun trackedIds(): Set<Long> = preferences.getStringSet("tracked_ids", emptySet()).orEmpty()
        .mapNotNull(String::toLongOrNull).toSet()

    fun read(id: Long, kind: String): ScheduledOccurrence? {
        val prefix = prefix(id, kind)
        val date = RoutineDate.parse(preferences.getString("${prefix}date", null)) ?: return null
        val token = preferences.getString("${prefix}token", null) ?: return null
        return ScheduledOccurrence(
            id, kind, date, preferences.getLong("${prefix}time", 0L),
            preferences.getString("${prefix}zone", "") ?: "",
            preferences.getString("${prefix}configuration", "") ?: "", token
        )
    }

    fun write(value: ScheduledOccurrence): Boolean {
        val prefix = prefix(value.routineId, value.kind)
        return preferences.edit()
            .putString("${prefix}date", value.date.iso)
            .putString("${prefix}token", value.token)
            .putLong("${prefix}time", value.triggerAtMillis)
            .putString("${prefix}zone", value.timeZoneId)
            .putString("${prefix}configuration", value.configuration)
            .putStringSet("tracked_ids", (trackedIds() + value.routineId).map(Long::toString).toSet())
            .commit()
    }

    fun forgetSchedule(id: Long, kind: String) {
        val prefix = prefix(id, kind)
        val editor = preferences.edit()
        listOf("date", "token", "time", "zone", "configuration").forEach { editor.remove(prefix + it) }
        check(editor.commit()) { "Alarm metadata could not be saved" }
    }

    fun lastRegularDate(id: Long): RoutineDate? =
        RoutineDate.parse(preferences.getString("delivered_date:$id", null))

    /** Keep a bounded civil-date history so a timezone round trip cannot repeat yesterday's alert. */
    fun deliveredDate(id: Long, date: RoutineDate): RoutineDate? =
        if (lastRegularDate(id) == date || preferences.getStringSet("delivered_dates:$id", emptySet()).orEmpty().contains(date.iso)) date else null

    fun activeToken(id: Long): String? = preferences.getString("active_token:$id", null)
    fun activeDate(id: Long): RoutineDate? = RoutineDate.parse(preferences.getString("active_date:$id", null))
    fun activeConfiguration(id: Long): String? = preferences.getString("active_configuration:$id", null)
    fun activeTimeZone(id: Long): String? = preferences.getString("active_zone:$id", null)

    /** Claim delivery before notifying, so repeated broadcasts cannot issue a second alert. */
    fun claimDelivery(value: ScheduledOccurrence): Boolean {
        val editor = preferences.edit()
            .putString("active_token:${value.routineId}", value.token)
            .putString("active_date:${value.routineId}", value.date.iso)
            .putString("active_configuration:${value.routineId}", value.configuration)
            .putString("active_zone:${value.routineId}", value.timeZoneId)
        if (value.kind == AlarmIdentity.REGULAR) {
            val dates = preferences.getStringSet("delivered_dates:${value.routineId}", emptySet()).orEmpty().toSet() +
                listOfNotNull(lastRegularDate(value.routineId)?.iso, value.date.iso)
            editor.putString("delivered_date:${value.routineId}", value.date.iso)
                .putStringSet("delivered_dates:${value.routineId}", dates.sortedDescending().take(32).toSet())
        }
        return editor.commit()
    }

    fun dismiss(id: Long) {
        check(preferences.edit().remove("active_token:$id").remove("active_date:$id")
            .remove("active_configuration:$id").remove("active_zone:$id").commit()) {
            "Alarm metadata could not be saved"
        }
    }

    fun remove(id: Long, preserveDeliveryHistory: Boolean = false) {
        val editor = preferences.edit()
        for (kind in listOf(AlarmIdentity.REGULAR, AlarmIdentity.SNOOZE)) {
            listOf("date", "token", "time", "zone", "configuration").forEach {
                editor.remove(prefix(id, kind) + it)
            }
        }
        editor.remove("active_token:$id").remove("active_date:$id")
            .remove("active_configuration:$id").remove("active_zone:$id")
        if (!preserveDeliveryHistory) editor.remove("delivered_date:$id").remove("delivered_dates:$id")
        editor.putStringSet("tracked_ids", (trackedIds() - id).map(Long::toString).toSet())
        check(editor.commit()) { "Alarm metadata could not be saved" }
    }

    private fun prefix(id: Long, kind: String) = "$kind:$id:"
}
