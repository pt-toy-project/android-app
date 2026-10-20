package com.example.test.timetable

import com.example.test.Routine.RoutineDate
import java.util.Locale

/** One weekly class meeting; independent of routines, completion and rewards. */
data class TimetableEntry(
    val id: Long = 0, val title: String, val weekday: Int,
    val startMinute: Int, val endMinute: Int, val location: String = "", val instructor: String = ""
) {
    init {
        require(id >= 0 && title.trim().length in 1..120 && weekday in 1..7)
        require(startMinute in 0..1439 && endMinute in 1..1440 && startMinute < endMinute)
        require(location.length <= 160 && instructor.length <= 80)
        require(listOf(title, location, instructor).none { '\u0000' in it })
    }
    val identity get() = listOf(title.trim(), weekday, startMinute, endMinute, location.trim())
    val timeLabel get() = "${time(startMinute)}–${time(endMinute)}"
    fun normalized() = copy(title = title.trim(), location = location.trim(), instructor = instructor.trim())
    fun overlaps(other: TimetableEntry) = weekday == other.weekday && startMinute < other.endMinute && other.startMinute < endMinute
    companion object {
        val weekdays = listOf("월", "화", "수", "목", "금", "토", "일")
        fun weekday(date: RoutineDate) = date.week().indexOf(date) + 1
        fun time(minute: Int) = String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60)
        fun minute(value: String): Int {
            require(value.matches(Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]|24:00")))
            return value.take(2).toInt() * 60 + value.takeLast(2).toInt()
        }
    }
}
