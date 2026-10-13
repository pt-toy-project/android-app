package com.example.test.alarm

import com.example.test.Routine.RoutineDate
import com.example.test.data.UserRoutine

/** The same checks protect ordinary and snoozed deliveries, including stale update broadcasts. */
internal object AlarmDeliveryPolicy {
    fun configuration(routine: UserRoutine): String = "${routine.time}|${routine.weekdayMask}|${routine.title}"

    fun actionMatchesSnapshot(
        routine: UserRoutine?,
        activeConfiguration: String?,
        activeTimeZoneId: String?,
        currentTimeZoneId: String
    ): Boolean = routine != null && !routine.deleted && routine.enabled && routine.alarmEnabled &&
        activeConfiguration == configuration(routine) && activeTimeZoneId == currentTimeZoneId

    fun snoozeStillValid(
        routine: UserRoutine,
        occurrence: ScheduledOccurrence,
        today: RoutineDate,
        currentTimeZoneId: String,
        alreadyCompleted: Boolean,
        lastRegularDate: RoutineDate?
    ): Boolean = occurrence.kind == AlarmIdentity.SNOOZE && occurrence.routineId == routine.id &&
        actionMatchesSnapshot(routine, occurrence.configuration, occurrence.timeZoneId, currentTimeZoneId) &&
        occurrence.date == today && occurrence.date.iso >= routine.createdOn &&
        lastRegularDate == occurrence.date && !alreadyCompleted &&
        NextOccurrenceCalculator.occursOn(routine.weekdayMask, occurrence.date)

    fun eligible(
        routine: UserRoutine?,
        occurrence: ScheduledOccurrence,
        today: RoutineDate,
        nowMillis: Long,
        timeZoneId: String,
        globallyEnabled: Boolean,
        alreadyCompleted: Boolean,
        lastRegularDate: RoutineDate?
    ): Boolean {
        if (routine == null || routine.id != occurrence.routineId || routine.deleted ||
            !routine.enabled || !routine.alarmEnabled || !globallyEnabled || alreadyCompleted) return false
        if (occurrence.date != today || occurrence.date.iso < routine.createdOn ||
            occurrence.triggerAtMillis > nowMillis || occurrence.timeZoneId != timeZoneId ||
            occurrence.configuration != configuration(routine) ||
            !NextOccurrenceCalculator.occursOn(routine.weekdayMask, occurrence.date)) return false
        return when (occurrence.kind) {
            AlarmIdentity.REGULAR -> lastRegularDate != occurrence.date
            AlarmIdentity.SNOOZE -> lastRegularDate == occurrence.date
            else -> false
        }
    }
}
