package com.example.test.Routine

import com.example.test.data.RoutineRules

/** Display choices never bypass repository validation or alter an adoption. */
object TimelinePresentation {
    fun forHero(rows: List<PersonalRoutineRow>, heroId: String?): List<PersonalRoutineRow> =
        rows.filter { heroId == null || it.routine.heroId == heroId }
            .sortedWith(compareBy({ it.routine.time }, { it.routine.id }))

    fun next(rows: List<PersonalRoutineRow>): PersonalRoutineRow? =
        rows.firstOrNull { !it.completed && it.routine.enabled }

    fun canComplete(row: PersonalRoutineRow): Boolean = RoutineRules.completionFailure(
        row.routine, row.date, row.today, row.completed, 0L
    ) == null
}
