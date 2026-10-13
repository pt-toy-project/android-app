package com.example.test.alarm

import com.example.test.Routine.RoutineDate

/** Data URIs, rather than truncated Long request codes or mutable extras, identify each intent. */
object AlarmIdentity {
    const val REGULAR = "regular"
    const val SNOOZE = "snooze"
    const val COMPLETE = "complete"

    fun uri(kind: String, routineId: Long): String {
        require(kind in setOf(REGULAR, SNOOZE, COMPLETE, "open", "snooze_action"))
        require(routineId > 0)
        return "historyroutine://$kind/$routineId"
    }

    fun token(routineId: Long, date: RoutineDate, kind: String, triggerAtMillis: Long): String {
        require(routineId > 0 && kind in setOf(REGULAR, SNOOZE))
        return "$routineId:${date.iso}:$kind:$triggerAtMillis"
    }

    fun snoozeAt(nowMillis: Long): Long? = runCatching { Math.addExact(nowMillis, 5 * 60_000L) }.getOrNull()
}
