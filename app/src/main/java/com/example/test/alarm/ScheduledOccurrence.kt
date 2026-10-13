package com.example.test.alarm

import com.example.test.Routine.RoutineDate

internal data class ScheduledOccurrence(
    val routineId: Long,
    val kind: String,
    val date: RoutineDate,
    val triggerAtMillis: Long,
    val timeZoneId: String,
    val configuration: String,
    val token: String
)
