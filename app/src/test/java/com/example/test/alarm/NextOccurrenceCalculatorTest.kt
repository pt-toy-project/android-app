package com.example.test.alarm

import com.example.test.Routine.RoutineDate
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class NextOccurrenceCalculatorTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun instant(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0,
        zone: TimeZone = utc): Long = GregorianCalendar(zone).apply {
        clear(); set(year, month - 1, day, hour, minute, 0)
    }.timeInMillis

    @Test fun futureTimeUsesTodayAndEqualityUsesNextDay() {
        val before = instant(2026, 10, 12, 7, 59)
        assertEquals(NextOccurrence(RoutineDate(2026, 10, 12), instant(2026, 10, 12, 8)),
            NextOccurrenceCalculator.next("08:00", 127, before, utc))
        assertEquals(RoutineDate(2026, 10, 13), NextOccurrenceCalculator.next("08:00", 127,
            instant(2026, 10, 12, 8), utc)?.date)
    }

    @Test fun mondayBitAndWeeklyWrapAreCorrect() {
        assertEquals(RoutineDate(2026, 10, 19), NextOccurrenceCalculator.next("08:00", 1,
            instant(2026, 10, 12, 9), utc)?.date)
        assertEquals(RoutineDate(2026, 10, 18), NextOccurrenceCalculator.next("08:00", 64,
            instant(2026, 10, 12, 9), utc)?.date)
    }

    @Test fun selectedTuesdayAndThursdayUseNearestDay() {
        assertEquals(RoutineDate(2026, 10, 13), NextOccurrenceCalculator.next("08:00", 2 or 8,
            instant(2026, 10, 12, 9), utc)?.date)
        assertEquals(RoutineDate(2026, 10, 15), NextOccurrenceCalculator.next("08:00", 2 or 8,
            instant(2026, 10, 13, 9), utc)?.date)
    }

    @Test fun invalidInputCannotCreateAnAlarm() {
        val now = instant(2026, 10, 12)
        for (time in listOf("8:00", "24:00", "12:60", "aa:bb", "", "08:0", "-1:00")) {
            assertNull(time, NextOccurrenceCalculator.next(time, 127, now, utc))
        }
        for (mask in listOf(0, -1, 128, 256)) {
            assertNull(NextOccurrenceCalculator.next("08:00", mask, now, utc))
        }
    }

    @Test fun leapDayAndYearBoundaryStayCivilDates() {
        assertEquals(RoutineDate(2024, 2, 29), NextOccurrenceCalculator.next("00:00", 127,
            instant(2024, 2, 28, 23, 59), utc)?.date)
        assertEquals(RoutineDate(2027, 1, 1), NextOccurrenceCalculator.next("00:00", 127,
            instant(2026, 12, 31, 23, 59), utc)?.date)
    }

    @Test fun localTimeZoneChangesNextOccurrence() {
        val now = instant(2026, 10, 12, 10)
        val seoul = TimeZone.getTimeZone("Asia/Seoul")
        val inSeoul = NextOccurrenceCalculator.next("20:00", 127, now, seoul)
        val inUtc = NextOccurrenceCalculator.next("20:00", 127, now, utc)
        assertEquals(instant(2026, 10, 12, 11), inSeoul?.triggerAtMillis)
        assertEquals(instant(2026, 10, 12, 20), inUtc?.triggerAtMillis)
    }

    @Test fun dstGapMovesNonexistentTimeForward() {
        val berlin = TimeZone.getTimeZone("Europe/Berlin")
        val next = NextOccurrenceCalculator.next("02:30", 127, instant(2026, 3, 29, 0), berlin)
        assertEquals(RoutineDate(2026, 3, 29), next?.date)
        assertEquals(instant(2026, 3, 29, 1, 30), next?.triggerAtMillis)
        val local = GregorianCalendar(berlin).apply { timeInMillis = requireNotNull(next).triggerAtMillis }
        assertEquals(3, local.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, local.get(Calendar.MINUTE))
    }

    @Test fun dstOverlapUsesLaterInstanceOnce() {
        val berlin = TimeZone.getTimeZone("Europe/Berlin")
        val next = NextOccurrenceCalculator.next("02:30", 127, instant(2026, 10, 25, 0), berlin)
        assertEquals(instant(2026, 10, 25, 1, 30), next?.triggerAtMillis)
        assertEquals(RoutineDate(2026, 10, 26), NextOccurrenceCalculator.next("02:30", 127,
            requireNotNull(next).triggerAtMillis, berlin)?.date)
    }

    @Test fun repeatingDaysFollowWallClockAcrossDstNotFixed24Hours() {
        val berlin = TimeZone.getTimeZone("Europe/Berlin")
        val first = requireNotNull(NextOccurrenceCalculator.next("08:00", 127,
            instant(2026, 3, 28, 0), berlin))
        val second = requireNotNull(NextOccurrenceCalculator.next("08:00", 127, first.triggerAtMillis, berlin))
        assertEquals(23 * 60 * 60_000L, second.triggerAtMillis - first.triggerAtMillis)
    }

    @Test fun skippedPacificCivilDayNeverGetsAnotherDaysWeekday() {
        val apia = TimeZone.getTimeZone("Pacific/Apia")
        val next = requireNotNull(NextOccurrenceCalculator.next("08:00", 1 shl 4,
            instant(2011, 12, 29, 9, 0, apia), apia))
        assertEquals(RoutineDate(2012, 1, 6), next.date)
    }

    @Test fun occurrenceWeekdayMatchesMondayBitConvention() {
        assertTrue(NextOccurrenceCalculator.occursOn(1, RoutineDate(2026, 10, 12)))
        assertFalse(NextOccurrenceCalculator.occursOn(64, RoutineDate(2026, 10, 12)))
        assertTrue(NextOccurrenceCalculator.occursOn(64, RoutineDate(2026, 10, 18)))
        assertFalse(NextOccurrenceCalculator.occursOn(0, RoutineDate(2026, 10, 18)))
    }
}
