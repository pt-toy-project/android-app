package com.example.test.Routine

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/** A civil date, independent of a clock time or UTC offset. Months are 1-based. */
data class RoutineDate(val year: Int, val month: Int, val day: Int) : Comparable<RoutineDate> {
    init {
        require(year in 1..9999) { "Year out of range" }
        calendar().timeInMillis // Reject invalid dates instead of silently rolling into another month.
    }

    val iso: String get() = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)

    fun plusDays(days: Int): RoutineDate = calendar().apply {
        add(Calendar.DAY_OF_MONTH, days)
    }.let(::fromCalendar)

    fun week(): List<RoutineDate> {
        val offset = (calendar().get(Calendar.DAY_OF_WEEK) + 5) % 7
        val monday = plusDays(-offset)
        return (0..6).map(monday::plusDays)
    }

    fun label(pattern: String): String = java.text.SimpleDateFormat(pattern, Locale.KOREAN).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(calendar().timeInMillis))

    override fun compareTo(other: RoutineDate): Int =
        compareValuesBy(this, other, RoutineDate::year, RoutineDate::month, RoutineDate::day)

    private fun calendar() = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
        gregorianChange = Date(Long.MIN_VALUE)
        isLenient = false
        clear()
        set(year, month - 1, day)
    }

    companion object {
        fun today(timeZone: TimeZone = TimeZone.getDefault()): RoutineDate =
            fromCalendar(GregorianCalendar(timeZone))

        fun parse(value: String?): RoutineDate? {
            if (value == null || !value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null
            return runCatching {
                RoutineDate(value.substring(0, 4).toInt(), value.substring(5, 7).toInt(), value.substring(8).toInt())
            }.getOrNull()
        }

        fun fromCalendar(calendar: Calendar) = RoutineDate(
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}
