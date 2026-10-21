package com.example.test.timetable

import com.example.test.Routine.RoutineDate

/** A short, date-specific preview for the home screen. */
data class TodayTimetablePreview(val classes: List<TimetableEntry>, val remaining: Int) {
    companion object {
        fun forDate(entries: List<TimetableEntry>, date: RoutineDate, limit: Int = 2): TodayTimetablePreview {
            require(limit > 0)
            val matching = entries.filter { it.weekday == TimetableEntry.weekday(date) }
                .sortedWith(compareBy<TimetableEntry> { it.startMinute }.thenBy { it.title })
            return TodayTimetablePreview(matching.take(limit), (matching.size - limit).coerceAtLeast(0))
        }
    }
}
