package com.example.test.Routine

import com.example.test.data.UserRoutine
import org.junit.Assert.*
import org.junit.Test

class TimelinePresentationTest {
    private val today = RoutineDate(2026, 10, 13)
    private fun row(id: Long, hero: String, time: String, completed: Boolean = false) = PersonalRoutineRow(
        UserRoutine(id, hero, "${hero}_block", "실천", time, 127, true, false, today.iso),
        completed, today, today, 600
    )
    @Test fun heroSelectionSortsTimeWithoutChangingOtherAdoptions() {
        val rows = listOf(row(3, "sejong", "21:00"), row(2, "newton", "08:00"), row(1, "sejong", "08:00"))
        val original = rows.toList()
        assertEquals(listOf(1L, 2L, 3L), TimelinePresentation.forHero(rows, null).map { it.routine.id })
        assertEquals(listOf(1L, 3L), TimelinePresentation.forHero(rows, "sejong").map { it.routine.id })
        assertTrue(TimelinePresentation.forHero(rows, "unknown").isEmpty())
        assertEquals(original, rows)
    }
    @Test fun nextActionSkipsCompletedAndPausedBlocks() {
        val done = row(1, "sejong", "07:00", true)
        val paused = row(2, "sejong", "08:00").let { it.copy(routine = it.routine.copy(enabled = false)) }
        val next = row(3, "sejong", "09:00")
        assertEquals(next, TimelinePresentation.next(listOf(done, paused, next)))
        assertNull(TimelinePresentation.next(listOf(done, paused)))
    }
    @Test fun inlineCompletionKeepsDateAdoptionAndEnabledGuards() {
        val valid = row(1, "sejong", "09:00")
        assertTrue(TimelinePresentation.canComplete(valid))
        assertFalse(TimelinePresentation.canComplete(valid.copy(completed = true)))
        assertFalse(TimelinePresentation.canComplete(valid.copy(date = today.plusDays(1))))
        assertFalse(TimelinePresentation.canComplete(valid.copy(date = today.plusDays(-1))))
        assertFalse(TimelinePresentation.canComplete(valid.copy(routine = valid.routine.copy(enabled = false))))
        assertFalse(TimelinePresentation.canComplete(valid.copy(routine = valid.routine.copy(deleted = true))))
        assertFalse(TimelinePresentation.canComplete(valid.copy(routine = valid.routine.copy(weekdayMask = 1))))
    }
}
