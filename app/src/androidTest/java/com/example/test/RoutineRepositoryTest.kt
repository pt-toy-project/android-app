package com.example.test

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.test.Routine.*
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineRepositoryTest {
    private lateinit var context: Context
    private lateinit var fileName: String
    private val today = RoutineDate(2026, 10, 12)
    @Before fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        fileName = "routine_test_${UUID.randomUUID()}"
    }
    @After fun cleanUp() { context.deleteSharedPreferences(fileName) }
    private fun repository() = RoutineRepository(context, fileName)

    @Test fun completionRestoresWithItsHeroAndDateOnly() {
        val scope = RoutineScope("sejong", today)
        assertEquals(CompletionResult.RECORDED, repository().complete(scope, 2, today))
        val restored = repository()
        assertTrue(restored.routines(scope).single { it.id == 2 }.isCompleted)
        assertTrue(restored.routines(RoutineScope("einstein", today)).none { it.isCompleted })
        assertTrue(restored.routines(RoutineScope("sejong", today.plusDays(-1))).none { it.isCompleted })
    }

    @Test fun duplicateFromDifferentRepositoriesKeepsOneCompletion() {
        val scope = RoutineScope("sejong", today)
        assertEquals(CompletionResult.RECORDED, repository().complete(scope, 1, today))
        assertEquals(CompletionResult.ALREADY_COMPLETED, repository().complete(scope, 1, today))
        assertEquals(1, repository().routines(scope).count { it.isCompleted })
    }

    @Test fun invalidSelectedHeroFallsBackAndValidSelectionRestores() {
        repository().selectedHeroId = "unknown"
        assertEquals("sejong", repository().selectedHeroId)
        repository().selectedHeroId = "einstein"
        assertEquals("einstein", repository().selectedHeroId)
    }

    @Test fun futureCompletionDoesNotPersist() {
        val scope = RoutineScope("sejong", today.plusDays(1))
        assertEquals(CompletionResult.FUTURE_DATE, repository().complete(scope, 1, today))
        assertTrue(repository().routines(scope).none { it.isCompleted })
    }
}
