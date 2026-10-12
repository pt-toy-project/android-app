package com.example.test.Routine

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RoutineTrackerTest {
    private class MemoryStore : CompletionStore {
        val values = mutableMapOf<String, Set<Int>>()
        var writes = 0
        override fun read(key: String) = values[key].orEmpty()
        override fun write(key: String, completedIds: Set<Int>) { values[key] = completedIds.toSet(); writes++ }
    }
    private val today = RoutineDate(2026, 10, 12)

    @Test fun completionsDoNotLeakAcrossHeroesOrDates() {
        val store = MemoryStore()
        val tracker = RoutineTracker(store)
        val scope = RoutineScope("sejong", today)
        assertEquals(CompletionResult.RECORDED, tracker.complete(scope, 1, today))
        assertTrue(tracker.routines(scope).first().isCompleted)
        assertFalse(tracker.routines(RoutineScope("einstein", today)).first().isCompleted)
        assertFalse(tracker.routines(RoutineScope("sejong", today.plusDays(-1))).first().isCompleted)
    }

    @Test fun duplicateCompletionWritesOnlyOnce() {
        val store = MemoryStore()
        val tracker = RoutineTracker(store)
        val scope = RoutineScope("sejong", today)
        assertEquals(CompletionResult.RECORDED, tracker.complete(scope, 1, today))
        assertEquals(CompletionResult.ALREADY_COMPLETED, tracker.complete(scope, 1, today))
        assertEquals(1, store.writes)
        assertEquals(1, tracker.routines(scope).count { it.isCompleted })
    }

    @Test fun futureAndUnknownRoutinesCannotChangeStoredState() {
        val store = MemoryStore()
        val tracker = RoutineTracker(store)
        assertEquals(CompletionResult.FUTURE_DATE, tracker.complete(RoutineScope("sejong", today.plusDays(1)), 1, today))
        assertEquals(CompletionResult.UNKNOWN_ROUTINE, tracker.complete(RoutineScope("sejong", today), 999, today))
        assertEquals(0, store.writes)
    }

    @Test fun newControllerRestoresStateFromItsStore() {
        val store = MemoryStore()
        val scope = RoutineScope("sejong", today)
        RoutineTracker(store).complete(scope, 3, today)
        val restored = RoutineTracker(store).routines(scope)
        assertEquals(listOf(3), restored.filter { it.isCompleted }.map { it.id })
        assertTrue(restored.filterNot { it.id == 3 }.none { it.isCompleted })
    }

    @Test fun emptyScheduleIsSafeAndRejectsCompletions() {
        val store = MemoryStore()
        val tracker = RoutineTracker(store, emptyList())
        val scope = RoutineScope("sejong", today)
        assertTrue(tracker.routines(scope).isEmpty())
        assertEquals(CompletionResult.UNKNOWN_ROUTINE, tracker.complete(scope, 1, today))
    }

    @Test fun futureRecordBecomesAvailableWhenItsDateArrives() {
        val tracker = RoutineTracker(MemoryStore())
        val scope = RoutineScope("sejong", today.plusDays(1))
        assertEquals(CompletionResult.FUTURE_DATE, tracker.complete(scope, 1, today))
        assertEquals(CompletionResult.RECORDED, tracker.complete(scope, 1, today.plusDays(1)))
        assertTrue(tracker.routines(scope).first().isCompleted)
    }

    @Test fun concurrentDuplicateRequestsRecordOnlyOnce() {
        val store = MemoryStore()
        val tracker = RoutineTracker(store)
        val scope = RoutineScope("sejong", today)
        val executor = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)
        try {
            val requests = (1..20).map {
                executor.submit(Callable {
                    check(start.await(5, TimeUnit.SECONDS))
                    tracker.complete(scope, 1, today)
                })
            }
            start.countDown()
            val results = requests.map { it.get(5, TimeUnit.SECONDS) }
            assertEquals(1, results.count { it == CompletionResult.RECORDED })
            assertEquals(19, results.count { it == CompletionResult.ALREADY_COMPLETED })
            assertEquals(1, store.writes)
        } finally {
            executor.shutdownNow()
        }
    }
}
