package com.example.test.Routine

/** Calendar math uses civil dates, so DST and the device's clock time cannot move a day. */
object ActivityCalendar {
    fun monthStart(date: RoutineDate) = RoutineDate(date.year, date.month, 1)
    fun moveMonth(date: RoutineDate, delta: Int): RoutineDate {
        val index = (date.year - 1) * 12 + date.month - 1 + delta
        require(index in 0 until 9999 * 12)
        val first = RoutineDate(index / 12 + 1, index % 12 + 1, 1)
        return RoutineDate(first.year, first.month, date.day.coerceAtMost(days(first).size))
    }
    fun days(date: RoutineDate): List<RoutineDate> {
        val first = monthStart(date)
        return (1..31).mapNotNull { day -> RoutineDate.parse("${first.iso.take(8)}${day.toString().padStart(2, '0')}") }
    }
    fun leadingBlanks(date: RoutineDate): Int {
        val first = monthStart(date)
        // RoutineDate.week() is Monday-first; the calendar follows the same order.
        return first.week().indexOf(first)
    }
}

data class DayActivity(
    val date: RoutineDate,
    val planned: Int = 0,
    val completed: Int = 0,
    val checksPlanned: Int = 0,
    val checked: Int = 0,
    val coins: Long = 0
) {
    val routineGoal get() = maxOf(planned, completed)
    val checkGoal get() = maxOf(checksPlanned, checked)
    val routineProgress get() = if (routineGoal == 0) 0f else completed.toFloat() / routineGoal
    val checkProgress get() = if (checkGoal == 0) 0f else checked.toFloat() / checkGoal
    val active get() = completed > 0 || checked > 0
}
