package com.example.test.Routine

import org.junit.Assert.*
import org.junit.Test

class ActivityCalendarTest {
    @Test fun leapDaysAndMonthEdgesStayValid() {
        assertEquals(29, ActivityCalendar.days(RoutineDate(2024, 2, 1)).size)
        assertEquals(28, ActivityCalendar.days(RoutineDate(2025, 2, 1)).size)
        assertEquals(RoutineDate(2024, 2, 29), ActivityCalendar.moveMonth(RoutineDate(2024, 1, 31), 1))
        assertEquals(RoutineDate(2025, 1, 31), ActivityCalendar.moveMonth(RoutineDate(2024, 12, 31), 1))
        assertEquals(RoutineDate(2024, 12, 31), ActivityCalendar.moveMonth(RoutineDate(2025, 1, 31), -1))
    }
    @Test fun mondayFirstGridAlignsEveryDay() {
        val first = RoutineDate(2026, 10, 1)
        assertEquals(3, ActivityCalendar.leadingBlanks(first))
        assertEquals(0, ActivityCalendar.leadingBlanks(RoutineDate(2026, 6, 1)))
        assertEquals(6, ActivityCalendar.leadingBlanks(RoutineDate(2026, 11, 1)))
    }
    @Test fun emptyAndHistoricalProgressNeverDivideByZeroOrLoseRecords() {
        val date = RoutineDate(2026, 10, 1)
        assertEquals(0f, DayActivity(date).routineProgress)
        assertFalse(DayActivity(date).active)
        val historic = DayActivity(date, completed = 2, checked = 3)
        assertEquals(2, historic.routineGoal)
        assertEquals(3, historic.checkGoal)
        assertEquals(1f, historic.routineProgress)
        assertTrue(historic.active)
    }
}
