package com.example.test.Routine

import org.junit.Assert.*
import org.junit.Test
import java.util.GregorianCalendar
import java.util.TimeZone

class RoutineDateTest {
    @Test fun weekCrossesYearAndStartsOnMonday() {
        val week = RoutineDate(2026, 1, 1).week()
        assertEquals("2025-12-29", week.first().iso)
        assertEquals("2026-01-04", week.last().iso)
        assertEquals(7, week.toSet().size)
    }

    @Test fun sundayBelongsToPreviousMonday() {
        assertEquals("2026-01-05", RoutineDate(2026, 1, 11).week().first().iso)
    }

    @Test fun leapDayIsKeptAndInvalidDatesAreRejected() {
        assertEquals("2024-02-29", RoutineDate(2024, 2, 28).plusDays(1).iso)
        assertEquals("2024-03-01", RoutineDate(2024, 2, 29).plusDays(1).iso)
        listOf(null, "2025-02-29", "2026-13-01", "2026-00-01", "2026-01-00", "26-01-01", "2026-01-01junk").forEach {
            assertNull(it, RoutineDate.parse(it))
        }
    }

    @Test fun timezoneBoundaryUsesCivilDateInsteadOfUtcDate() {
        val utc = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(2026, 0, 1, 15, 30)
        }
        val seoul = GregorianCalendar(TimeZone.getTimeZone("Asia/Seoul")).apply { timeInMillis = utc.timeInMillis }
        assertEquals("2026-01-01", RoutineDate.fromCalendar(utc).iso)
        assertEquals("2026-01-02", RoutineDate.fromCalendar(seoul).iso)
    }
}
