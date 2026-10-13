package com.example.test.alarm

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.example.test.Routine.RoutineDate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Real Android SharedPreferences checks; compilation alone is not a passing device result. */
class AlarmStoreTest {
    private lateinit var context: Context
    private lateinit var store: AlarmStore
    private val filename = "alarm_store_test"
    private val date = RoutineDate(2026, 10, 12)
    private val regular = ScheduledOccurrence(1L, AlarmIdentity.REGULAR, date, 1000L,
        "Asia/Seoul", "08:00|127|읽기", AlarmIdentity.token(1, date, AlarmIdentity.REGULAR, 1000))

    @Before fun before() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteSharedPreferences(filename)
        store = AlarmStore(context, filename)
    }
    @After fun after() { context.deleteSharedPreferences(filename) }

    @Test fun scheduledOccurrenceSurvivesReopeningAndKeepsLongId() {
        val value = regular.copy(routineId = 1L + (1L shl 32))
        assertTrue(store.write(value))
        val reopened = AlarmStore(context, filename)
        assertEquals(value, reopened.read(value.routineId, value.kind))
        assertEquals(setOf(value.routineId), reopened.trackedIds())
    }

    @Test fun reschedulingReplacesTokenInsteadOfAddingAnotherIdentity() {
        assertTrue(store.write(regular))
        val replacement = regular.copy(triggerAtMillis = 2000L, token = "replacement")
        assertTrue(store.write(replacement))
        assertEquals(replacement, store.read(1L, AlarmIdentity.REGULAR))
        assertEquals(setOf(1L), store.trackedIds())
    }

    @Test fun regularAndSnoozeAreSeparateAndCancelingOnePreservesTheOther() {
        val snooze = regular.copy(kind = AlarmIdentity.SNOOZE, triggerAtMillis = 301000L, token = "snooze")
        assertTrue(store.write(regular)); assertTrue(store.write(snooze))
        store.forgetSchedule(1, AlarmIdentity.REGULAR)
        assertNull(store.read(1, AlarmIdentity.REGULAR))
        assertEquals(snooze, store.read(1, AlarmIdentity.SNOOZE))
    }

    @Test fun dismissingAnAlertRetainsDailyDedupeHistory() {
        assertTrue(store.claimDelivery(regular))
        store.dismiss(1)
        assertNull(store.activeToken(1))
        assertNull(store.activeDate(1))
        assertEquals(date, store.lastRegularDate(1))
    }

    @Test fun claimKeepsSnapshotForRejectingEditedOrOldNotificationActions() {
        assertTrue(store.claimDelivery(regular))
        assertEquals(regular.token, store.activeToken(1))
        assertEquals(regular.configuration, store.activeConfiguration(1))
        assertEquals(regular.timeZoneId, store.activeTimeZone(1))
        assertEquals(date, store.activeDate(1))
    }

    @Test fun timezoneRoundTripRetainsBothCivilDatesForDedupe() {
        assertTrue(store.claimDelivery(regular))
        val priorLocalDay = regular.copy(date = date.plusDays(-1), token = "prior-day")
        assertTrue(store.claimDelivery(priorLocalDay))
        assertEquals(date, store.deliveredDate(1, date))
        assertEquals(date.plusDays(-1), store.deliveredDate(1, date.plusDays(-1)))
        assertNull(store.deliveredDate(1, date.plusDays(1)))
    }

    @Test fun cancellationAndReenableRetainDailyDedupeHistory() {
        assertTrue(store.write(regular)); assertTrue(store.claimDelivery(regular))
        store.remove(1, preserveDeliveryHistory = true)
        assertNull(store.read(1, AlarmIdentity.REGULAR))
        assertNull(store.activeToken(1))
        assertEquals(date, store.lastRegularDate(1))
        assertTrue(store.write(regular))
        assertEquals(date, store.lastRegularDate(1))
    }

    @Test fun deletionClearsSchedulesAndActiveStateWithoutTouchingAnotherRoutine() {
        assertTrue(store.write(regular)); assertTrue(store.claimDelivery(regular))
        val other = regular.copy(routineId = 2L, token = "other")
        assertTrue(store.write(other))
        store.remove(1)
        assertNull(store.read(1, AlarmIdentity.REGULAR)); assertNull(store.activeToken(1))
        assertNull(store.lastRegularDate(1))
        assertEquals(other, store.read(2, AlarmIdentity.REGULAR))
        assertEquals(setOf(2L), store.trackedIds())
    }
}
