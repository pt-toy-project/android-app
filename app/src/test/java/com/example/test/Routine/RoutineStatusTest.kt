package com.example.test.Routine

import org.junit.Assert.*
import org.junit.Test

class RoutineStatusTest {
    private val today = RoutineDate(2026, 10, 12)
    private val routine = Routine(1, "18:00", "저녁 식사")

    @Test fun futureDateIsUpcomingEvenAfterItsClockTime() {
        assertEquals(RoutineStatus.UPCOMING, routine.statusOn(today.plusDays(1), today, RoutineTime(23, 0)))
    }

    @Test fun pastDateIsOverdueEvenBeforeItsClockTime() {
        assertEquals(RoutineStatus.OVERDUE, routine.statusOn(today.plusDays(-1), today, RoutineTime(6, 0)))
    }

    @Test fun scheduledMinuteIsDue() {
        assertEquals(RoutineStatus.UPCOMING, routine.statusOn(today, today, RoutineTime(17, 59)))
        assertEquals(RoutineStatus.OVERDUE, routine.statusOn(today, today, RoutineTime(18, 0)))
    }

    @Test fun completedWinsOverDateAndClock() {
        assertEquals(RoutineStatus.COMPLETED, routine.copy(isCompleted = true).statusOn(today.plusDays(-1), today, RoutineTime(23, 0)))
    }

    @Test fun invalidClockInputIsRejected() {
        listOf("24:00", "06:60", "6:00", "garbage", "-1:00").forEach { assertNull(RoutineTime.parse(it)) }
        assertEquals(0, RoutineTime.parse("00:00")?.minutes)
        assertEquals(1439, RoutineTime.parse("23:59")?.minutes)
    }
}
