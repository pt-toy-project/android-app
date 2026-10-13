package com.example.test.alarm

import com.example.test.Routine.RoutineDate
import com.example.test.data.UserRoutine
import org.junit.Assert.*
import org.junit.Test

class AlarmDeliveryPolicyTest {
    private val date = RoutineDate(2026, 10, 12)
    private val routine = UserRoutine(4L, "sejong", "reading", "읽기", "08:00", 127, true, true, "2026-10-01")
    private val occurrence = ScheduledOccurrence(4L, AlarmIdentity.REGULAR, date, 1000L,
        "Asia/Seoul", AlarmDeliveryPolicy.configuration(routine), "4:2026-10-12:regular:1000")

    private fun allowed(routine: UserRoutine? = this.routine, occurrence: ScheduledOccurrence = this.occurrence,
        today: RoutineDate = date, now: Long = 1000L, zone: String = "Asia/Seoul", global: Boolean = true,
        complete: Boolean = false, delivered: RoutineDate? = null) = AlarmDeliveryPolicy.eligible(
        routine, occurrence, today, now, zone, global, complete, delivered
    )

    @Test fun validOccurrenceIsEligibleAtDueTime() { assertTrue(allowed()) }
    @Test fun duplicateRegularDeliveryIsRejected() { assertFalse(allowed(delivered = date)) }
    @Test fun noDeliveryBeforeTriggerOrAfterOriginalDay() {
        assertFalse(allowed(now = 999L))
        assertFalse(allowed(today = date.plusDays(1)))
    }
    @Test fun completedRoutineCannotDeliverOrSnooze() {
        assertFalse(allowed(complete = true))
        assertFalse(allowed(occurrence = occurrence.copy(kind = AlarmIdentity.SNOOZE), delivered = date, complete = true))
    }
    @Test fun disabledDeletedAndMissingRoutinesCannotDeliver() {
        assertFalse(allowed(routine = null))
        assertFalse(allowed(routine = routine.copy(enabled = false)))
        assertFalse(allowed(routine = routine.copy(alarmEnabled = false)))
        assertFalse(allowed(routine = routine.copy(deleted = true)))
        assertFalse(allowed(global = false))
    }
    @Test fun updateOrTimezoneChangeMakesPendingBroadcastStale() {
        assertFalse(allowed(routine = routine.copy(time = "09:00")))
        assertFalse(allowed(routine = routine.copy(weekdayMask = 1)))
        assertFalse(allowed(zone = "UTC"))
        assertFalse(allowed(routine = routine.copy(id = 5L)))
    }
    @Test fun unrelatedWeekdayAndPreAdoptionDateAreRejected() {
        val otherDayRoutine = routine.copy(weekdayMask = 2)
        assertFalse(allowed(routine = otherDayRoutine, occurrence = occurrence.copy(configuration = AlarmDeliveryPolicy.configuration(otherDayRoutine))))
        assertFalse(allowed(routine = routine.copy(createdOn = "2026-10-13")))
    }
    @Test fun explicitSnoozeRequiresAnAlreadyDeliveredOriginalOccurrence() {
        val snooze = occurrence.copy(kind = AlarmIdentity.SNOOZE)
        assertFalse(allowed(occurrence = snooze))
        assertTrue(allowed(occurrence = snooze, delivered = date))
        assertFalse(allowed(occurrence = snooze, delivered = date.plusDays(-1)))
    }
    @Test fun dueButDelayedSnoozeSurvivesReconcileUntilConsumedOrCompleted() {
        val snooze = occurrence.copy(kind = AlarmIdentity.SNOOZE)
        assertTrue(AlarmDeliveryPolicy.snoozeStillValid(routine, snooze, date, "Asia/Seoul", false, date))
        assertFalse(AlarmDeliveryPolicy.snoozeStillValid(routine, snooze, date, "Asia/Seoul", true, date))
        assertFalse(AlarmDeliveryPolicy.snoozeStillValid(routine, snooze, date.plusDays(1), "Asia/Seoul", false, date))
        assertFalse(AlarmDeliveryPolicy.snoozeStillValid(routine, snooze, date, "UTC", false, date))
    }

    @Test fun oldNotificationActionsAreRejectedAfterEditDisableOrTimezoneChange() {
        val configuration = AlarmDeliveryPolicy.configuration(routine)
        assertTrue(AlarmDeliveryPolicy.actionMatchesSnapshot(routine, configuration, "Asia/Seoul", "Asia/Seoul"))
        assertFalse(AlarmDeliveryPolicy.actionMatchesSnapshot(routine.copy(time = "09:00"), configuration, "Asia/Seoul", "Asia/Seoul"))
        assertFalse(AlarmDeliveryPolicy.actionMatchesSnapshot(routine.copy(enabled = false), configuration, "Asia/Seoul", "Asia/Seoul"))
        assertFalse(AlarmDeliveryPolicy.actionMatchesSnapshot(routine.copy(alarmEnabled = false), configuration, "Asia/Seoul", "Asia/Seoul"))
        assertFalse(AlarmDeliveryPolicy.actionMatchesSnapshot(routine, configuration, "Asia/Seoul", "UTC"))
        assertFalse(AlarmDeliveryPolicy.actionMatchesSnapshot(null, configuration, "Asia/Seoul", "Asia/Seoul"))
    }

    @Test fun longIdsAndActionTypesNeverSharePendingIntentIdentity() {
        val largeId = 4L + (1L shl 32)
        val identities = listOf(4L, largeId, Long.MAX_VALUE).flatMap { id ->
            listOf(AlarmIdentity.REGULAR, AlarmIdentity.SNOOZE, AlarmIdentity.COMPLETE, "open", "snooze_action")
                .map { kind -> AlarmIdentity.uri(kind, id) }
        }
        assertEquals(identities.size, identities.toSet().size)
    }
    @Test fun repeatedOccurrenceTokenIsStableAndDifferentDayOrSnoozeDiffers() {
        assertEquals(AlarmIdentity.token(4, date, AlarmIdentity.REGULAR, 1000), AlarmIdentity.token(4, date, AlarmIdentity.REGULAR, 1000))
        assertNotEquals(AlarmIdentity.token(4, date, AlarmIdentity.REGULAR, 1000), AlarmIdentity.token(4, date.plusDays(1), AlarmIdentity.REGULAR, 1000))
        assertNotEquals(AlarmIdentity.token(4, date, AlarmIdentity.REGULAR, 1000), AlarmIdentity.token(4, date, AlarmIdentity.SNOOZE, 1000))
    }
    @Test fun snoozeIsFiveMinutesAndOverflowCannotWrapIntoPast() {
        assertEquals(301_000L, AlarmIdentity.snoozeAt(1000L))
        assertNull(AlarmIdentity.snoozeAt(Long.MAX_VALUE))
    }
}
