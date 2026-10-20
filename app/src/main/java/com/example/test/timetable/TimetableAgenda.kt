package com.example.test.timetable

/** A class starting now is active until its end; a finished class is never the next action. */
object TimetableAgenda {
    data class State(val today: List<TimetableEntry>, val current: TimetableEntry?, val next: TimetableEntry?)
    fun at(entries: List<TimetableEntry>, weekday: Int, minute: Int): State {
        require(weekday in 1..7 && minute in 0..1439)
        val today = entries.filter { it.weekday == weekday }.sortedBy { it.startMinute }
        val current = today.firstOrNull { it.startMinute <= minute && it.endMinute > minute }
        return State(today, current, current ?: today.firstOrNull { it.startMinute > minute })
    }
}
