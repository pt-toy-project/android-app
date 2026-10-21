package com.example.test.Routine

import com.example.test.data.RoutineRules
import com.example.test.data.RoutineSource

/** Display choices never bypass repository validation or alter an adoption. */
object TimelinePresentation {
    fun forHero(rows: List<PersonalRoutineRow>, heroId: String?): List<PersonalRoutineRow> =
        rows.filter { heroId == null || it.routine.heroId == heroId }
            .sortedWith(compareBy({ it.routine.time }, { it.routine.id }))

    /** Today combines both sources; a copied block keeps one place in the flow. */
    fun dailyFlow(rows: List<PersonalRoutineRow>, heroId: String?, managementMode: Boolean = false): List<PersonalRoutineRow> =
        rows.filter { it.routine.source == RoutineSource.PERSONAL || heroId == null || it.routine.heroId == heroId }
            .sortedWith(compareBy<PersonalRoutineRow> { it.routine.source != RoutineSource.PERSONAL }
                .thenBy { it.routine.id })
            .let { if (managementMode) it else it.distinctBy { row -> row.routine.heroId to row.routine.templateId } }
            .sortedWith(compareBy({ it.routine.time }, { it.routine.id }))

    fun next(rows: List<PersonalRoutineRow>): PersonalRoutineRow? =
        rows.firstOrNull { !it.completed && it.routine.enabled }

    fun canComplete(row: PersonalRoutineRow): Boolean = RoutineRules.completionFailure(
        row.routine, row.date, row.today, row.completed, 0L
    ) == null
}
