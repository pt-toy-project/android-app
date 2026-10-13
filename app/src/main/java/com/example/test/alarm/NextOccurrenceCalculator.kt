package com.example.test.alarm

import com.example.test.Routine.RoutineDate
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/** One civil-date occurrence. Repeating alarms are scheduled individually, not every 24 hours. */
data class NextOccurrence(val date: RoutineDate, val triggerAtMillis: Long)

object NextOccurrenceCalculator {
    const val EVERY_DAY = 0b1111111

    /** Monday is bit 0. The result is strictly after [afterMillis].
     *
     * The reminder follows the device's local wall clock. GregorianCalendar advances a nonexistent
     * DST time by the transition gap and chooses the later (standard-time) instance of an overlap.
     * A skipped civil date is not silently assigned another weekday's reminder.
     */
    fun next(
        time: String,
        weekdayMask: Int,
        afterMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): NextOccurrence? {
        if (weekdayMask !in 1..EVERY_DAY || !time.matches(Regex("\\d{2}:\\d{2}"))) return null
        val hour = time.substring(0, 2).toIntOrNull() ?: return null
        val minute = time.substring(3, 5).toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        val day = calendar(timeZone).apply {
            timeInMillis = afterMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        repeat(8) {
            if (day.get(Calendar.YEAR) !in 1..9999) return null
            val weekdayBit = (day.get(Calendar.DAY_OF_WEEK) + 5) % 7
            if (weekdayMask and (1 shl weekdayBit) != 0) {
                val intendedDate = RoutineDate.fromCalendar(day)
                val candidate = calendar(timeZone).apply {
                    clear()
                    set(intendedDate.year, intendedDate.month - 1, intendedDate.day, hour, minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val actualDate = RoutineDate.fromCalendar(candidate)
                if (actualDate == intendedDate && candidate.timeInMillis > afterMillis) {
                    return NextOccurrence(actualDate, candidate.timeInMillis)
                }
            }
            day.add(Calendar.DAY_OF_MONTH, 1)
        }
        return null
    }

    fun occursOn(weekdayMask: Int, date: RoutineDate): Boolean {
        if (weekdayMask !in 1..EVERY_DAY) return false
        val calendar = calendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(date.year, date.month - 1, date.day, 12, 0, 0)
        }
        return weekdayMask and (1 shl ((calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7)) != 0
    }

    private fun calendar(timeZone: TimeZone) = GregorianCalendar(timeZone, Locale.ROOT).apply {
        gregorianChange = Date(Long.MIN_VALUE)
        isLenient = true
    }
}
